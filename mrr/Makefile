
all: mrr

mrr: mrr.c
	gcc -Wall -Werror -g -pthread $< -o $@

clean:
	rm -f *.o mrr

docker:
	docker build --tag mrr:latest .
