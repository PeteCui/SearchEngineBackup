#!/bin/sh
java -cp classes -Xmx1g ir.Engine -d dataset/davisWiki -l ir22.png -p patterns.txt
