// Taken from https://banu.com/blog/2/how-to-use-epoll-a-complete-example-in-c/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/epoll.h>
#include <errno.h>

#define MAXEVENTS 64
#define BUF_SIZE 1024
char buf[BUF_SIZE];
char hbuf[NI_MAXHOST], sbuf[NI_MAXSERV];

// Run the server "./a.out 5000". Open several consoles and run "nc 127.0.0.1 
// 50000". Type some characters and hit return. Observe the server behaviour.

static void print_error_and_exit(const char* api_name) {
  perror(api_name);
  exit(EXIT_FAILURE);
}

static void make_socket_non_blocking(int sfd) {
  int flags;

  flags = fcntl(sfd, F_GETFL, 0);
  if (flags == -1) {
    print_error_and_exit("fcntl");
  }

  flags |= O_NONBLOCK;
  int ret = fcntl(sfd, F_SETFL, flags);
  if (ret == -1) {
    print_error_and_exit("fcntl");
  }
}

static void add_to_epoll_for_read(int efd, int fd) {
  struct epoll_event event;
  event.data.fd = fd;
  event.events = EPOLLIN;
  int ret = epoll_ctl(efd, EPOLL_CTL_ADD, fd, &event);
  if (ret == -1) {
    print_error_and_exit("epoll_ctl");
  }
}

static int create_and_bind(const char *port) {
  struct addrinfo hints;
  struct addrinfo *res;
  memset(&hints, 0, sizeof(struct addrinfo));
  // IPv4 and IPv6
  hints.ai_family = AF_UNSPEC;			
  // TCP socket
  hints.ai_socktype = SOCK_STREAM;	
  // All interfaces
  hints.ai_flags = AI_PASSIVE;      
  int ret = getaddrinfo(NULL, port, &hints, &res);
  if (ret != 0) {
    fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(ret));
    exit(EXIT_FAILURE);
  }

  // Loop over adding and try to bind
  int sfd;
  struct addrinfo *rp;
  for (rp = res; rp != NULL; rp = rp->ai_next) {
    sfd = socket(rp->ai_family, rp->ai_socktype, rp->ai_protocol);
    if (sfd == -1) {
      continue;
    }
    ret = bind(sfd, rp->ai_addr, rp->ai_addrlen);
    if (ret == 0) {
      break;
    }
    close(sfd);
  }

  if (rp == NULL) {
    fprintf(stderr, "could not bind\n");
    exit(EXIT_FAILURE);
  }

  freeaddrinfo(res);
  return sfd;
}

static int serve(const char* port) {
  int sfd = create_and_bind(port);
  make_socket_non_blocking(sfd);
  int ret = listen(sfd, SOMAXCONN);
  if (ret == -1) {
    print_error_and_exit("listen");
  }
  return sfd;
}

// We have data on the fd waiting to be read. Read and display it. We must read
// whatever data is available completely, as we are running in edge-triggered
// mode and won't get a notification again for the same data.
static void read_all(int fd) {
  int done = 0;
  ssize_t count;
  int ret;

  for (;;) {
    count = read(fd, buf, sizeof(buf));
    if (count == -1) {
      // errno == EAGAIN means we have read all data. So ignore it.
      if (errno != EAGAIN) {
        perror("read");
        done = 1;
      }
      break;
    } else if (count == 0) {
      // End of file. The remote has closed the connection.
      done = 1;
      break;
    } 

    buf[count] = 0;
    printf("read %ld bytes: %s\n", count, buf);
  }

  if (done) {
    // Closing the descriptor will make epoll remove it
    // from the set of descriptors which are monitored.
    close(fd);
    printf("file descriptor %d closed\n", fd);
  }
}

static void accept_for_read(int efd, int sfd) {
  struct sockaddr in_addr;
  socklen_t in_len = sizeof(in_addr);
  int infd;
  int ret;

  for (;;) {
    infd = accept(sfd, &in_addr, &in_len);
    if (infd == -1) {
      if ((errno == EAGAIN) || (errno == EWOULDBLOCK)) {
        // We have processed all incoming connections.
        break;
      } else {
        perror("accept");
        break;
      }
    }
    ret = getnameinfo(&in_addr, in_len, 
        hbuf, sizeof hbuf,
        sbuf, sizeof sbuf,
        NI_NUMERICHOST | NI_NUMERICSERV);
    if (ret == 0) {
      printf("accepted connection on descriptor %d (host=%s, port=%s)\n", infd, hbuf, sbuf);
    }
    make_socket_non_blocking(infd);add_to_epoll_for_read(efd, infd);
  }
}

int main(int argc, const char *argv[]) {
  if (argc != 2) {
    fprintf(stderr, "usage: %s [port]\n", argv[0]);
    exit(EXIT_FAILURE);
  }
  int sfd = serve(argv[1]);


  int efd = epoll_create1(0);
  if (efd == -1) {
    print_error_and_exit("epoll_create1");
  }
  add_to_epoll_for_read(efd, sfd);

  struct epoll_event* events = calloc(MAXEVENTS, sizeof(struct epoll_event));

  int nfds;
  int fd;

  for (;;) {
    nfds = epoll_wait(efd, events, MAXEVENTS, -1);
    if (nfds == -1) {
      print_error_and_exit("epoll_wait");
    }
    for (int i = 0; i < nfds; i++) {
      fd = events[i].data.fd;
      if (events[i].events & EPOLLERR) {
        // An error has occurred on this fd, or the socket is not
        // ready for reading (why were we notified then?)
        fprintf(stderr, "epoll error\n");
        close(fd);
        continue;
      } else if (sfd == fd) {
        // We have a notification on the listening socket, which
        // means one or more incoming connections.
        accept_for_read(efd, sfd);
        continue;
      } else {
        read_all(fd);
      }
    }
  }

  free(events);
  close(sfd);
  return EXIT_SUCCESS;
}
