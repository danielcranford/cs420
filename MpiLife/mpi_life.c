#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <mpi.h>

#define CELL char


 
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
    Allocates a new array
*/
ARRAY2D newArray(int length, int width) {
    ARRAY2D result;
    result.length = length;
    result.width = width;
    result.data = malloc(length * width); 
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
int rowsLowerBound(int rows, int size, int rank) {
    int rowsPerNode = rows / size;
    return rank * rowsPerNode;
}

/** 
    Calculates the upper bound (exclusive) of the rows this node is responsible 
    for
*/
int rowsUpperBound(int rows, int size, int rank) {
    int rowsPerNode = rows / size;
    // assign final node (rank+1=size) to handle any left over rows after even 
    // division
    return (rank+1==size) ? rows : (rank+1) * rowsPerNode;
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
        MPI_Ssend(sendBuff, sendLen, MPI_CHAR, sendTo, 0, MPI_COMM_WORLD);
        MPI_Recv(recvBuff, recvLen, MPI_CHAR, recvFrom, 0, MPI_COMM_WORLD, &status);
    } else {
        MPI_Recv(recvBuff, recvLen, MPI_CHAR, recvFrom, 0, MPI_COMM_WORLD, &status);
        MPI_Ssend(sendBuff, sendLen, MPI_CHAR, sendTo, 0, MPI_COMM_WORLD);
    }
}

/**
    Loads initial configuration at process 0 and sends it to other processes
*/ 
void setupArrays(const char* configFile, ARRAY2D arrays[2], int* pUpper, int* pLower) {
    int lengthWidth[2] = {0};
    int size, rank;

    // get the size process group and the rank of this process
    MPI_Comm_size(MPI_COMM_WORLD, &size);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);

    // load initial configuration at process 0
    if(rank == 0) {
        arrays[0] = readArray(configFile);
        arrays[1] = newArray(arrays[0].length, arrays[0].width);
        // check the size and number of processes - we currently only support even decompositions
        if(arrays[0].length % size != 0) {
            // TODO: used MPI_Scatterv and MPI_Gatherv to support uneven decompositions
            printf("This program does not support uneven decompositions. The number of rows (%d) \n"
                   "should be divisible by the number of processes (%d)\n", arrays[0].length, size);
            MPI_Abort(MPI_COMM_WORLD, -1);
        }
        lengthWidth[0] = arrays[0].length;
        lengthWidth[1] = arrays[0].width;
    }

    // broadcast the size of the array to everyone
    MPI_Bcast(lengthWidth, 2, MPI_INT, 0, MPI_COMM_WORLD);  

    // non root nodes must allocate buffer space
    if(rank != 0) {
        arrays[0] = newArray(lengthWidth[0], lengthWidth[1]);
        arrays[1] = newArray(lengthWidth[0], lengthWidth[1]);
    }

    // calculate bounds
    *pLower = rowsLowerBound(arrays[0].length, size, rank);
    *pUpper = rowsUpperBound(arrays[0].length, size, rank);    

    // if running on more than one process
    if(size > 1) {
        // TODO: requires an even size decomposition
        // send configuration to each process
        /*MPI_Scatter(
                arrays[0].data, (*pUpper-*pLower)*arrays[0].width, MPI_CHAR,
                ARRAY2D_PTR(arrays[0],*pLower,0), (*pUpper-*pLower)*arrays[0].width, MPI_CHAR,
                0, MPI_COMM_WORLD);*/
        // each process needs not only its own chunk, but also the row above and
        // below it, so the simple MPI_Scatter call is insufficient
        // Broadcasting the entire config is an inefficient solution, but it's 
        // also simple 
        MPI_Bcast(arrays[0].data, arrays[0].length*arrays[0].width, MPI_CHAR, 0, MPI_COMM_WORLD);  

    }

}

/**
    Update neighbors with adjacent rows if more than one process
*/
void updateOtherProcesses(int upper, int lower, ARRAY2D array) {
    int size, rank;

    // get the size process group and the rank of this process
    MPI_Comm_size(MPI_COMM_WORLD, &size);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
        
    if(size > 1) {
        int upRank =  (rank + 1) % size;
        int downRank = MOD_POS(rank - 1, size);
        int sendFirst = rank % 2 == 0;

        // current node is responsible for rows [lower, upper)
        // so send row upper-1 to upRank who stores it in lower-1
        // and send row lower to downRank who stores it in upper
        // send updates up
        swapUpdates(
            ARRAY2D_PTR(array,upper-1,0), array.width, upRank, 
            ARRAY2D_PTR(array,lower-1,0), array.width, downRank,
            sendFirst);
        // send updates down
        swapUpdates(
            ARRAY2D_PTR(array,lower,0), array.width, downRank, 
            ARRAY2D_PTR(array,upper,0), array.width, upRank,
            sendFirst);
    
        // TODO: requires even size decomposition
        // send entire grid back to process 0 for displaying        
        MPI_Gather(
            ARRAY2D_PTR(array,lower,0), (upper-lower)*array.width, MPI_CHAR,
            array.data, (upper-lower)*array.width, MPI_CHAR,
            0, MPI_COMM_WORLD);
    }
}

/**
    Display the current status process 0
*/
void display(ARRAY2D array, int generation) {
    int rank;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  
    if(rank == 0) {
        printf("Generation %d\n", generation);
        printArray(array);
    }
}

/**
*/
void mainLoop(const char* configFile, int generations) {
    // need a pair of arrays, one to hold grid and the other to hold updates 
    //(unless we invent a more complex datastructure/algorithm to allow in-place
    // modification of the array)    
    ARRAY2D arrays[2] = {0};
    // keep track of which array is active, the other is used for updates
    int active = 0;
    int i;
    // bounds on the rows this node handles
    int upper, lower;

    setupArrays(configFile, arrays, &upper, &lower);    

    // loop over number of generations
    for(i = 0; i < generations; i++) {
        display(arrays[active], i);

        // calculate updates
        timestep(arrays[active], arrays[!active], lower, upper);

        // swap arrays
        active = !active;

        updateOtherProcesses(upper, lower, arrays[active]);
    }

    display(arrays[active], i);

    // cleanup
    freeArray(arrays[0]);
    freeArray(arrays[1]);
}


/**
    Main program entry point. Accepts two optional command line arguments
    mpi_life configFile generations
    configFile is the path to an initial configuration file in the format 
        expected by readArray
    generations is the number of generations to run for
*/
int main(int argc, char** argv) {
    int generations = 64;
    char* configFile = "glider.txt";
    
    MPI_Init(&argc, &argv);

    // parse command line args
    if(argc == 3) {
        configFile = argv[1];
        generations = atol(argv[2]);
    }
    
    mainLoop(configFile, generations);

    MPI_Finalize();    
    return 0;
}
