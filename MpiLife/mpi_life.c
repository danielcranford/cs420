#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <mpi.h>

#define CELL char

CELL grid[16][16] = {
    {0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
    {1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}};
CELL updates[16][16] = {0};

 
typedef struct _ARRAY2D {
    int length;
    int width;
    char* data;
} ARRAY2D;


// defines a modulo function that always returns values within the range [0,y)
// instead of returning negative values for negative values of x as the c % 
// operator returns
#define MOD_POS(x,y) ((x) >= 0 ? (x) % (y) : (x) % (y) + (y))
// returns a pointer to the data begining at index [i][j]
#define ARRAY2D_PTR(ar,i,j) ((ar).data + ((MOD_POS((i),(ar).length)) * (ar).width) + (MOD_POS((j),(ar).width)))
// returns r or l value of the data value stored in the array at index [i][j] 
// with i and j taken mod the length/width of the array to ensure they are 
// within range
#define ARRAY2D_INDEX(ar,i,j) (*(ARRAY2D_PTR(ar,i,j)))
// define characters used to represent alive/dead in input/output
#define ALIVECH '*'
#define DEADCH '.'

/** 
    Reads in an array from a file. The file format is 
    
    length width
    array lines consisting of '.' for 0 and '*' for 1

    Example input file

    2 3
    ..*
    *..

    todo, no error checking :(
*/
ARRAY2D readArray(const char* name) {
    FILE* f = NULL;
    ARRAY2D result = {0};
    int i, j;

    // open file
    f = fopen(name, "r");
    if(f == NULL) {
        perror("Unable to open array file");
        exit(-1);
    }

    // read header line
    fscanf(f, "%d %d\n", &result.length, &result.width);
    
    // allocate result data buffer    
    result.data = malloc(result.length * result.width);

    // read in the file lines
    for(i = 0; i < result.length; i++) {
        for(j = 0; j < result.width; j++) {
            int c = fgetc(f);
            if(c == DEADCH) {
                ARRAY2D_INDEX(result, i, j) = 0;
            } else if (c == ALIVECH) {
                ARRAY2D_INDEX(result, i, j) = 1;
            } else {
                printf("file format error %c\n", c);
                exit(-1);
            }
        }
        fgetc(f);   // consume endline
    }

    // close file
    fclose(f);

    return result;    
}

/**
    Frees dynamically allocated memory associates with the given array.
*/
void freeArray(ARRAY2D ar) {
    free(ar.data);
}

/**
    Creates a copy of the given array.
*/
ARRAY2D copyArray(ARRAY2D ar) {
    ARRAY2D result = ar;
    result.data = malloc(result.width * result.length);
    memcpy(result.data, ar.data, result.width * result.length);
    return result;
}

/**
    Prints an array to stdout.
*/
void printArray(ARRAY2D ar) {
    int i, j;
    for(i = 0; i < ar.length; i++) {
        for(j = 0; j < ar.width; j++) {
            putchar(ARRAY2D_INDEX(ar, i, j) ? ALIVECH : DEADCH);
        }
        putchar('\n');
    }
}

/**
    Calculates next step in evolution grid rows [startRow, stopRow) 
*/
void timestep(ARRAY2D grid, ARRAY2D updates, int startRow, int stopRow) {
    int i, j;
    for(i = startRow; i < stopRow; i++) {
        for(j = 0; j < grid.width; j++) {
            int neighbors = 
                ARRAY2D_INDEX(grid, i-1, j-1) + ARRAY2D_INDEX(grid, i-1, j) + ARRAY2D_INDEX(grid, i-1, j+1) +
                ARRAY2D_INDEX(grid, i, j-1) + ARRAY2D_INDEX(grid, i, j+1) +
                ARRAY2D_INDEX(grid, i+1, j-1) + ARRAY2D_INDEX(grid, i+1, j) + ARRAY2D_INDEX(grid, i+1, j+1);
            if(neighbors < 2 || neighbors > 3) {
                ARRAY2D_INDEX(updates,i, j) = 0;
            } else if(neighbors == 3) {
                ARRAY2D_INDEX(updates, i, j) = 1;
            } else {
                ARRAY2D_INDEX(updates, i, j) = ARRAY2D_INDEX(grid, i, j);
            }
        }
    }
}

/**
    Calculates the lower bound (inclusive) of the rows this node is responsible 
    for.
*/ 
int rowsLowerBound(ARRAY2D ar, int size, int rank) {
    int rowsPerNode = ar.length / size;
    return rank * rowsPerNode;
}

/** 
    Calculates the upper bound (exclusive) of the rows this node is responsible 
    for
*/
int rowsUpperBound(ARRAY2D ar, int size, int rank) {
    int rowsPerNode = ar.length / size;
    // assign final node (rank+1=size) to handle any left over rows after even 
    // division
    return (rank+1==size) ? ar.length : (rank+1) * rowsPerNode;
}

/**
    Swap a buffer with neighbor
    sendFirst - if sendFirst then send/recv else recv/send 
*/
void swapUpdates(void* sendBuff, int sendLen, int sendTo, void* recvBuff, int recvLen, int recvFrom, int sendFirst) {
    MPI_Status status = {0};

    // TODO: is it really better to do send/recv and order when MPI has SendRecv function?
    // SSend to deteck deadlocks
    if(sendFirst) {
        MPI_SSend(sendBuff, sendLen, MPI_CHAR, sendTo, 0, MPI_COMM_WORLD);
        MPI_Recv(recvBuff, recvLen, MPI_CHAR, recvFrom, 0, MPI_COMM_WORLD, &status);
    } else {
        MPI_Recv(recvBuff, recvLen, MPI_CHAR, recvFrom, 0, MPI_COMM_WORLD, &status);
        MPI_SSend(sendBuff, sendLen, MPI_CHAR, sendTo, 0, MPI_COMM_WORLD);
    }
}

/**
    size : valid values are 1, 2, 4, 16
    rank : 
*/
void f(int size, int rank, int generations) {
    // need a pair of arrays, one to hold grid and the other to hold updates 
    //(unless we invent a more complex datastructure/algorithm to allow in-place
    // modification of the array)    
    ARRAY2D arrays[2] = {0};
    // keep track of which array is active, the other is used for updates
    int active = 0;
    int i;
    // bounds on the rows this node handles
    int upper, lower;

    // calculate bounds, current process is reponsible for rows [lower, upper)
    lower = rowsLowerBound(arrays[active], size, rank);
    upper = rowsUpperBound(arrays[active], size, rank);    

    // load initial configuration at process 0
    if(rank == 0) {
        arrays[active] = readArray("initial_configuration.txt");
        arrays[!active] = copyArray(arrays[active]);
        // check the size and number of processes - we currently only support even decompositions
        if(arrays[active].length % size != 0) {
            printf("This program does not support uneven decompositions. The number of rows (%d) should be divisible by the number of processes (%d)\n", arrays[active].length, size);
            MPI_Abort(-1);
        } 
    }
    // if running on more than one process
    if(size > 1) {
        // TODO: requires an even size decomposition
        // send configuration to each process
        MPI_Scatter(
                arrays[active].data, (upper-lower)*arrays[active].width, MPI_CHAR,
                ARRAY2D_PTR(arrays[active],lower,0), (upper-lower)*arrays[active].width, MPI_CHAR,
                0, MPI_COMM_WORLD);
    }

    // loop over number of generations
    for(i = 0; i < generations; i++) {
        // calculate updates
        timestep(arrays[active], arrays[!active], lower, upper);

        // swap arrays
        active = !active;

        // update neighbors with adjacent rows if more than one process
        if(size > 1) {
            int upRank =  (rank + 1) % size;
            int downRank = MOD_POS(rank - 1, size);
            int sendFirst = rank % 2 == 0;

            // current node is responsible for rows [lower, upper)
            // so send row upper-1 to upRank who stores it in lower-1
            // and send row lower to downRank who stores it in upper
            // send updates up
            swapUpdates(
                ARRAY2D_PTR(arrays[active],upper-1,0), arrays[active].width, upRank, 
                ARRAY2D_PTR(arrays[active],lower-1,0), arrays[active].width, downRank,
                sendFirst);
            // send updates down
            swapUpdates(
                ARRAY2D_PTR(arrays[active],lower,0), arrays[active].width, upRank, 
                ARRAY2D_PTR(arrays[active],upper,0), arrays[active].width, downRank,
                sendFirst);
        
            // TODO: requires even size decomposition
            // send entire grid back to process 0 for displaying        
            MPI_Gather(
                ARRAY2D_PTR(arrays[active],lower,0), (upper-lower)*arrays[active].width, MPI_CHAR,
                arrays[active].data, (upper-lower)*arrays[active].width, MPI_CHAR,
                0, MPI_COMM_WORLD);
        }

        

        // display at process 0
        if(rank == 0) {
            printf("Generation %d\n", i);
            printArray(arrays[active]);
        }

    }

    // cleanup
    freeArray(arrays[0]);
    freeArray(arrays[1]);
}



int main(int argc, char** argv) {
    int size, rank, i;
    MPI_Init(&argc, &argv);
    MPI_Comm_size(MPI_COMM_WORLD, &size);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    printf("I am process %d of %d\n", rank, size);
    
    f(size, rank, 64);

    MPI_Finalize();    
    return 0;
}
