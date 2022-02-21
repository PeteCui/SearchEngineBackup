#!/bin/sh
java -cp classes -Xmx1g ir.Engine -d dataset/guardian -l ir22.png -p patterns.txt -ni
