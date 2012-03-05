import java.util.Random;

/**
 *
 * @author dgc30
 */
public class CoinFlip {
    
    private static void printUsage() {
        System.out.println("Usage: CoinFlip [-speedup|-scaleup] nthreads iterations");
        System.out.println(" -speedup test speedup by timing test with 1..nthreads threads");
        System.out.println(" -scaleup test scaleup by timing with 1..nthreads threads and ");
        System.out.println("          1*iterations..nthreads*iterations iterations");
    }
    
    public static void main(String[] args) throws InterruptedException {
        if(!(args.length == 2 || (args.length == 3 && args[0].matches("-speedup|-scaleup")))) {
            printUsage();
            return;
        }
        
        int numberThreads = Integer.parseInt(args[args.length - 2]);
        if(numberThreads < 1) {
            printUsage();
            return;
        }
        int numberIterations = Integer.parseInt(args[args.length - 1]);
        if(numberIterations < 1) {
            printUsage();
            return;
        }
        
        if(args[0].equals("-speedup")) {
            for(int i = 1; i <= numberThreads; i++) {
                timeTest(i, numberIterations);
            }
        } else if(args[0].equals("-scaleup")) {
            for(int i = 1; i <= numberThreads; i++) {
                timeTest(i, i*numberIterations);
            }            
        } else {
            timeTest(numberThreads, numberIterations);
        }
    }

    private static void timeTest(int numberThreads, int numberIterations) throws InterruptedException {
        CoinFlipperApp app = new CoinFlipperApp(numberThreads, numberIterations);
        app.runTest();
        System.out.printf("Heads: %,d Flips: %,d Threads: %,d Time: %,d ns Startup: %,d ns\n", app.getTotalHeads(), app.getTotalFlips(), numberThreads, app.getTotalTimeNs(), app.getStartupCostsNs());
    }
    
    
    private static class CoinFlipperTask implements Runnable {
        private int numberHeads = 0;
        private Random random = new Random();
        private int iterations;
        
        public CoinFlipperTask(int iterations) {
            this.iterations = iterations;
        }

        @Override
        public void run() {
            for(int i = 0; i < iterations; i++) {
                if(random.nextBoolean()) {
                    numberHeads++;
                }
            }
        }

        public int getIterations() {
            return iterations;
        }

        public int getNumberHeads() {
            return numberHeads;
        }
    }
    
    private static class CoinFlipperApp {
        private int totalHeads;
        private int totalFlips;
        private int numberThreads;
        private int numberIterations;
        private long startTimeNs;
        private long startupCostsNs;
        private long totalTimeNs;
        
        public CoinFlipperApp(int numberThreads, int numberIterations) {
            this.numberIterations = numberIterations;
            this.numberThreads = numberThreads;
        }
        
        public void runTest() throws InterruptedException {
            startTimeNs = System.nanoTime();
            Thread threads[] = new Thread[numberThreads];
            CoinFlipperTask tasks[] = new CoinFlipperTask[numberThreads + 1];

            for(int i = 0; i < numberThreads; i++) {
                tasks[i] = new CoinFlipperTask(numberIterations / numberThreads);
                threads[i] = new Thread(tasks[i]);
                threads[i].start();
            }
            startupCostsNs = System.nanoTime() - startTimeNs;
            
            // run any left over iterations in the main thread
            tasks[numberThreads] = new CoinFlipperTask(numberIterations % numberThreads);
            tasks[numberThreads].run();
            for(int i = 0; i < numberThreads; i++) {
                threads[i].join();
            }

            // sum the results of the tasks
            totalHeads = 0;
            totalFlips = 0;
            for(int i = 0; i < tasks.length; i++) {
                totalHeads += tasks[i].getNumberHeads();
                totalFlips += tasks[i].getIterations();
            }
            totalTimeNs = System.nanoTime() - startTimeNs;
        }

        public int getTotalFlips() {
            return totalFlips;
        }

        public int getTotalHeads() {
            return totalHeads;
        }

        public long getTotalTimeNs() {
            return totalTimeNs;
        }
        
        public long getStartupCostsNs() {
            return startupCostsNs;
        }
    }
    
}
