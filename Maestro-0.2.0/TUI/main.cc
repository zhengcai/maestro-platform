#include <ncurses.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

#include "parameters.hh"


int main(int argc, char *argv[])
{
  initscr();
  noecho();
  if(has_colors() == FALSE) {
      endwin();
      printf("Your terminal does not support color\n");
      exit(1);
    }
  start_color();
  init_pair(1, COLOR_RED, COLOR_BLACK);
  attron(COLOR_PAIR(1));
  char c;
  while ((c=getch()) != EOF) {
    clear();
    mvprintw(LINES / 2, 0, "MAX Y is %d, MAX X is %d, %d", LINES, COLS, c == RESIZE);
    refresh();
  }
  attroff(COLOR_PAIR(1));
  endwin();
  return 0;
}
