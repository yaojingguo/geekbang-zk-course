#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <netdb.h>
#include <stdlib.h>

#define BACKLOG 10
#define SIZE 1024
int port = 5000;

static void print_error_and_exit(const char* api_name) {
  perror(api_name);
  exit(EXIT_FAILURE);
}

static void echo(int sockfd) {
  char buf[SIZE] = {0};
  while (1) {
    printf("reading\n");
    int received = read(sockfd, buf, SIZE);
    printf("read %d bytes\n", received);
    if (received == 0) {
      printf("connection closed\n");
      break;
    }
    if (received == -1) {
      print_error_and_exit("read");
    }

    buf[received] = 0;
    printf("read %d byts: %s", received, buf);

    int position = 0;
    int ret;
    while (position < received) {
      int ret = write(sockfd, buf + position, received - position);
      if (ret == -1) {
        print_error_and_exit("write");
      }
      position += ret;
    }
  }
}

int main(int argc, char *argv[]) {
  int listenfd;
  if ((listenfd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
    print_error_and_exit("socket");
  }

  int reuse = 1;
  if (setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse)) == -1) {
    print_error_and_exit("setsockopt");
  }

  struct sockaddr_in serv_addr = {0}; 
  serv_addr.sin_family = AF_INET;
  serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
  serv_addr.sin_port = htons(port); 
  if (bind(listenfd, (struct sockaddr*)&serv_addr, sizeof(serv_addr)) == -1) {
    print_error_and_exit("bind");
  }
  printf("bound\n");
  if (listen(listenfd, BACKLOG) == -1) {
    print_error_and_exit("listen");
  }
  printf("listened\n");

  int connfd;
  printf("accepting\n");
  if ((connfd = accept(listenfd, (struct sockaddr*) NULL, NULL)) == -1) {
    print_error_and_exit("accept");
  }
  printf("accepted\n");

  echo(connfd);

  if (close(connfd) == -1) {
    print_error_and_exit("close");
  }
  if (close(listenfd) == -1) {
    print_error_and_exit("close");
  }

  return 0;
}
