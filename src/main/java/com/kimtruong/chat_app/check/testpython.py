import numpy as np
A = np.array([[1,2,-1,4],[-3,2,0,1],[2,1,3,-1],[-4,1,1,3]], float)
b = np.array([1,-2,3,-4], float)
print(np.linalg.solve(A, b))