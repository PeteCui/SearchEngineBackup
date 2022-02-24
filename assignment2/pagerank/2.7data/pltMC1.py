from lzma import MF_HC4
from re import X
import matplotlib.pyplot as plt

#MC1
x = [1, 2, 3, 4, 5]
y1 = [4.31720674e-06, 2.54976500e-06, 1.41493487e-06, 1.28040149e-06, 9.92448020e-07, ]
# plt.xlabel(" N * number of page times on random page ")
#MC2
x = [1, 2, 3, 4, 5]
y2 = [6.00340206e-06, 2.33548591e-06, 1.44684563e-06, 1.25804222e-06, 8.35046227e-07, ]
# plt.xlabel(" m * n times on each page ")
#MC3
x = [1, 2, 3, 4, 5]
y3 = [1.47208020e-06, 1.62796369e-06, 4.41008253e-07, 3.91976923e-07, 3.52842336e-07]
# plt.xlabel(" m * n times on each page but stop at sink page")

# #MC4
# x = [1, 2, 3, 4, 5]
y4 = [2.50482519e-06, 1.85122025e-06, 7.17313948e-07, 3.57239739e-07, 5.66032112e-07]

plt.ylabel("difference squared")
plt.plot(x, y1, marker=".", label="MC1")
plt.plot(x, y2, marker=".", label="MC2")
plt.plot(x, y3, marker=".", label="MC4")
plt.plot(x, y4, marker=".", label="MC5")
plt.legend()
plt.show()