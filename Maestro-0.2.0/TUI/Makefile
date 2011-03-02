CC = gcc
CPP = g++
DEBUG = -O3 -g
CFLAGS = -Wall -c $(DEBUG) $(INC)
LFLAGS = -Wall $(DEBUG) -Wl,-whole-archive -lncurses
OLIB = -Wl,-no-whole-archive -lpthread

TARGET = tui

SRC = main.cc

HEADERS = parameters.hh

tui : $(SRC:.cc=.o)
	$(CPP) $(LFLAGS) $^ $(OLIB) -o $@

%.o : %.cc $(HEADERS)
	$(CPP) $(CFLAGS) $<

clean :
	rm -f $(TARGET) *.o *~ .depend

