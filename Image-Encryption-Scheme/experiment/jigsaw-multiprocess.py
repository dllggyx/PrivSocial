import math
from os import listdir
from os.path import isfile, join
from scipy import misc
import numpy as np
import scipy
import glob
from PIL import Image
from time import time
from matplotlib import pyplot
from scipy.spatial import distance
from math import sqrt
import threading
import multiprocessing


data_directory = "./exper-img/"
piece_size = 64
image_size = 512
grid_size = image_size/piece_size
num_of_pieces = int((grid_size)**2)

photos = [data_directory+f for f in listdir(data_directory) if isfile(join(data_directory, f))]
photos = sorted(photos)



def score(pl, pr):
    return  MGC(pl, pr)


def MGC(pl, pr):
    s1 = MGCLR(pl, pr)
    s2 = MGCLR(np.fliplr(pr), np.fliplr(pl))
    return sqrt(s1 + s2)


def MGCLR(pl, pr):
    Gl = np.zeros((piece_size, 3))
    Gr = np.zeros((piece_size, 3))
    # TODO: vectorize this ================================================================ DONE!!!!
    #for c in range(3): # for three channels
     #   for i in range(piece_size):
      #      Gl[i, c] = pl[i, piece_size - 1, c] - pl[i, piece_size - 2, c]
    Gl = pl[:,piece_size-1,:] - pl[:,piece_size-2,:]
    uGl = np.mean(Gl, axis=0)
    # TODO: vectorize this ============================================================== DONE!!!!
   # for c in range(3): # for three channels
   #     for i in range(piece_size):
            #Gr[i, c] =  pr[i, 0, c] - pl[i, piece_size - 1, c]
    if pl.shape[1] == 64:
        Gr = pr[:,0,:] - pl[:,piece_size - 1,:]
    dummy = np.array([[0, 0, 0], [1,1,1], [-1,-1,-1], [0,0,1], [0,1,0], [1,0,0], [-1,0,0], [0, -1, 0], [0,0,-1]])
    cv = np.vstack((Gl, dummy))
    covGl = np.cov(np.transpose(cv))
    invCov = np.linalg.inv(covGl)
    ds = 0
    # TODO: apply zip maybe? I don't know really just saying! ========================== DONE!!!
    #for i in range(Gl.shape[0]):
     #   ds += distance.mahalanobis(Gl[i, :], Gr[i, :], invCov)
    X = np.vstack([Gl,Gr])
    KK = np.diag(np.sqrt(np.dot(np.dot((Gl-Gr),invCov),(Gl-Gr).T)))
#    print(KK.shape)
   # return ds
    return sum(KK)


def find_parent(node, parent):
    if parent[node] == node:
        return node
    parent[node] = find_parent(parent[node], parent)
    return parent[node]


def find_node_in_grid(grid, node):
        #print ("test on where",  grid.where(node))
    # TODO: maybe using np.where()? Generally Optimize this maybe =============================================== DONE!!!
    i,j = np.where(grid==node)
#    for i in range(grid.shape[0]):
 #       for j in range(grid.shape[1]):
  #          if grid[i, j] == node:
   #             return i,j
    return i,j


def find_translation(grid1, grid2, edge):
    node1x, node1y = find_node_in_grid(grid1, edge[1])
    node2x, node2y = find_node_in_grid(grid2, edge[2])
    translationx, translationy = node2x - node1x, node2y - node1y
    _tr = {"RL":(0, -1), "LR":(0, 1), "UD":(-1, 0), "DU":(1, 0)}
    translationx += _tr[edge[3][:2]][0]
    translationy += _tr[edge[3][:2]][1]
    return translationx, translationy


def rotate_grid(grid, edge):
    num_of_transpose = int(edge[3][-1])
    result = grid
    for i in range(num_of_transpose):
        result = np.transpose(result)
    return result


def check_merge(grid1, grid2, edge):
    #TODO: any smart way of doing this maybe? Think about positivity/negativity of numbers!
    translationx, translationy = find_translation(grid1, grid2, edge)
    for i in range(grid1.shape[0]):
        for j in range(grid1.shape[1]):
            if grid1[i, j] != -1:
                i2, j2 = i + translationx, j + translationy
                if(grid2[i2, j2] != -1):
                    return False
    return True

def merge(grid1, grid2, edge):
    translationx, translationy = find_translation(grid1, grid2, edge)
    for i in range(grid1.shape[0]):
        for j in range(grid1.shape[1]):
            if grid1[i, j] != -1:
                i2, j2 = i + translationx, j + translationy
                grid2[i2, j2] = grid1[i, j]
    return grid2


def kruskal(edges):
    parent = np.zeros(num_of_pieces, dtype=int)
    for i1 in range(num_of_pieces):
        parent[i1] = i1

    added_edges = 0
    tree_edges = []
    grids = [-1 * np.ones((2*num_of_pieces+1, 2*num_of_pieces+1), dtype=int) for _ in range(num_of_pieces)]

    for i in range(len(grids)):
        grids[i][num_of_pieces, num_of_pieces] = i

    while added_edges != num_of_pieces -1:
        current_edge = min(edges)
        edges.remove(current_edge)
        p1 = find_parent(current_edge[1], parent)
        p2 = find_parent(current_edge[2], parent)


        if p1 == p2:
            continue
        else:
            rotated_gridp1 = rotate_grid(grids[p1], current_edge)
            if check_merge(rotated_gridp1, grids[p2], current_edge):
                parent[p1] = p2
                tree_edges.append(current_edge)
                added_edges += 1
                merge(rotated_gridp1, grids[p2], current_edge)


    return tree_edges, grids, parent


def visualize_grid(g, pieces):
    def put_in(piece, x, y, frame, piece_size):
        #print "line 1", piece.shape, piece_size, piece_size*x, piece_size*(x+1), x, y
        #print "line 2", frame[piece_size*x:piece_size*(x+1), piece_size*y:piece_size*(y+1)].shape
        frame[piece_size*x:piece_size*(x+1), piece_size*y:piece_size*(y+1)] = piece


    frame = np.zeros((piece_size*g.shape[0],piece_size*g.shape[1],3))
    for i in range(g.shape[0]):
        for j in range(g.shape[1]):
            if g[i, j] != -1:
                put_in(pieces[g[i,j]], i, j, frame , piece_size)
    return frame


def normalize(edges, min_edges):
    ne = list()
    for edge in edges:
        ne.append((edge[0]/(min(min_edges[edge[1]], min_edges[edge[2]])), edge[1], edge[2], edge[3]))
    return ne

def create_permutation(grid):
    # TODO: grid does not have any -1 cell anymore. Just reshape and convert it to str and join it as it is! ============================= DONE!!!!!
    #
    perm = []
    A = np.asarray(grid).reshape(-1)
    CC = str(A).strip('[]')
    #print(CC)
#    perm.append(str(CC))
 #   for i in range(grid.shape[0]):
  #      for j in range(grid.shape[1]):
   #         if grid[i][j] != -1:
    #            perm.append(str(grid[i][j]))
#    return " ".join(perm)
    return CC


def maker():
    photo = multiprocessing.current_process().name
    edges = set()

    min_edges = -1 * np.ones(num_of_pieces)
    # image = misc.imread(photo)
    image = np.array(Image.open(photo))
    image = image.astype(float)
    print(photo)
    pieces = []

    for i in range(0, int(image_size/piece_size)):
        for j in range(0, int(image_size/piece_size)):
            pieces.append(image[i*piece_size:(i+1)*piece_size, j*piece_size:(j+1)*piece_size])
    errors = 0
    index1, index2 = 0, 0

    for _ in pieces:
        index2 = 0
        for pl in pieces:
            if index1 < index2:
                #print photo, index1, index2
                for it in range(0,4):
                    #print index1, index2, it
                    pr = pieces[index1]

                    print("pl shape:", pl.shape)
                    print("pr shape:", pr.shape)

                    DRL = score(pr, pl)
                    DLR = score(pl, pr)
                    prt = np.transpose(pr, (1, 0, 2))
                    prtf = np.fliplr(np.transpose(pr, (1, 0, 2)))
                    plt = np.transpose(pl, (1, 0, 2))
                    pltf = np.fliplr(np.transpose(pl, (1, 0, 2)))
                    DUD = score(prt, plt)
                    DDU = score(prtf, pltf)
                    node_edges = {"RL":DRL, "LR":DLR, "UD":DUD, "DU":DDU}
                    #print node_edges.__str__(), index1, index2
                    string = ""
                    minedge = min((DRL, DLR, DUD, DDU))
                    for index in range(piece_size):
                        if min_edges[index1] == -1 or minedge < min_edges[index1]:
                            min_edges[index1] = minedge
                        if min_edges[index2] == -1 or minedge < min_edges[index2]:
                            min_edges[index2] = minedge

                    pieces[index1] = np.transpose(pieces[index1], (1, 0 , 2))
                    edges.add((DRL, index1, index2, "RL"+str(it)))
                    edges.add((DLR, index1, index2, "LR"+str(it)))
                    edges.add((DUD, index1, index2, "UD"+str(it)))
                    edges.add((DDU, index1, index2, "DU"+str(it)))

            index2 += 1
        index1 += 1
    n_edges = normalize(edges, min_edges)
    tree_edges, grids, parent_list = kruskal(n_edges)
    trimmed_grid = trim_fill(grids[find_parent(0, parent_list)])
    frame = visualize_grid(trimmed_grid, pieces)
    scipy.misc.imsave(photo[-8:], frame)
    perm = create_permutation(trimmed_grid)
    #print perm, "output of create_permutation"
    final_result = ""
    final_result += photo[-8:] + '\n'
    final_result += perm
    ff = open("result"+photo[-8:]+".txt", 'w')
    ff.write(final_result)
    ff.close()


def trim_fill(grid):
    top_row, bottom_row, left_col, right_col = -1,-1,-1,-1
    for i in range(grid.shape[0]):
        for j in range(grid.shape[1]):
            if grid[i, j] != -1:
                top_row = min(top_row, i) if top_row != -1 else i
                bottom_row = max(bottom_row, i) if bottom_row != -1 else i
                left_col = min(left_col, j) if left_col != -1 else j
                right_col = max(right_col, j) if right_col != -1 else j
    grid = grid[top_row:bottom_row+1, left_col:right_col+1]
    #print grid
    mmin, mini, minj = -1, 0, 0
    #print "here is the range: ", range(grid.shape[0] - grid_size+1)
    for i in range(grid.shape[0] - grid_size+1):
        for j in range(grid.shape[1] - grid_size + 1):
            _ = (grid[i:i+grid_size,j:j+grid_size] == -1).sum()
            #print "here is the sum thing", grid[i:i+grid_size,j:j+grid_size].shape
            if mmin == -1 or mmin > _:
                mmin, mini, minj = _, i, j
    #print "best grid start ", mmin, mini, minj
    trimmed = []
    for i in range(grid.shape[0]):
        for j in range(grid.shape[1]):
            if grid[i, j] != -1 and (i < mini or i >= mini+grid_size or j < minj or j >= minj+grid_size):
                trimmed.append(grid[i, j])
    #print grid.shape
    #print "trimmed: ", trimmedtry:
    grid = grid[mini:mini+grid_size, minj:minj+grid_size]
    index = 0
    #print grid.shape
    for i in range(grid.shape[0]):
        for j in range(grid.shape[1]):
            if grid[i, j] == -1:
                grid[i, j] = trimmed[index]
                index += 1
                if index == len(trimmed):
                    break
    return gridy

if __name__ == '__main__':
    photo_index = 0
    while photo_index < len(photos):
        start_time = time()
        process = list()
        for i in range(7):
            if photo_index < len(photos):
                process.append(multiprocessing.Process(name=photos[photo_index], target=maker))
                process[-1].start()
                photo_index += 1

        for proc in process:
            proc.join()

        end_time = time()
        print(end_time - start_time)