PROGS=echo_client echo_server 

all: ${PROGS}

echo_client: echo_client.o

echo_server: echo_server.o

cscope:
	rm -fr cscope.*
	find -name '*.h' -or -name '*.c' > cscope.files
	cscope -b -q

clean:
	rm -f ${PROGS} *.o a.out
