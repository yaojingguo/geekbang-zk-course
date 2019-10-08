#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <arpa/inet.h> 

int port = 5000;
#define SIZE 1024

static void print_error_and_exit(const char* api_name) {
  perror(api_name);
  exit(EXIT_FAILURE);
}

static void write_all(int sockfd) {
  char msg[] = "hello\n";
  int position = 0;
  int count = sizeof(msg);
  int ret;
  while (position < count) {
    ret = write(sockfd, msg + position, count - position);
    if (ret == -1) {
      print_error_and_exit("write");
    }
    position += ret;
  }
}

static void read_all(int sockfd) {
  int ret;
  char buf[SIZE] = {0};
  while ((ret = read(sockfd, buf, SIZE)) > 0) {
    write(STDOUT_FILENO, buf, ret);
  }
  if (ret == -1) {
    print_error_and_exit("read");
  }
}

int main(int argc, char *argv[])
{
  int sockfd = 0;
  struct sockaddr_in serv_addr; 

  if (argc != 2) {
    fprintf(stderr, "Usage: %s <ip of server> \n",argv[0]);
    return EXIT_FAILURE;
  } 

  if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
    print_error_and_exit("socket");
  } 

  memset(&serv_addr, 0, sizeof(serv_addr)); 
  serv_addr.sin_family = AF_INET;
  serv_addr.sin_port = htons(port); 
  if (inet_pton(AF_INET, argv[1], &serv_addr.sin_addr) != 1) {
    print_error_and_exit("inet_pton");
  } 

  if (connect(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) == -1) {
    print_error_and_exit("connect");
  } 

  write_all(sockfd);
  read_all(sockfd);


  if (close(sockfd) == -1) {
    print_error_and_exit("close");
  }
  return 0;
}
