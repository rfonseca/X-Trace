#!/bin/sh
apxs2 -n xtrace -g 2>/dev/null

cp xtrace/Makefile .
cp xtrace/modules.mk .

sed -i -e 's/#DEFS.*/ifndef $(CXX)\n\tCXX=g++\nendif\n\nXTRACE=..\/..\/..\/..\/src\/cpp\/build/' Makefile
sed -i -e 's/#INCLUDES.*/INCLUDES=-I$(XTRACE)\/include/' Makefile
sed -i -e 's/#LIBS.*/LIBS=-L$(XTRACE)\/lib -lxtr-cpp/' Makefile

printf "Generated Makefile\n"
rm -rf xtrace
