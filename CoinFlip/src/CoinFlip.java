

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoinFlip {

    private static void printUsage() {
        System.out.println("Usage: CoinFlip [-speedup|-scaleup] [-s|-l] nthreads iterations");
        System.out.println(" -s use synchronized block instead of AtomicInteger");
        System.out.println(" -l use ReentrantLock instead of AtomicInteger");
        System.out.println(" -speedup test speedup by timing test with 1..nthreads threads");
        System.out.println(" -scaleup test scaleup by timing with 1..nthreads threads and ");
        System.out.println("          1*iterations..nthreads*iterations iterations");
    }

    interface CoinFlipper {

        public void flip(boolean heads);

        public int getNumberHeads();

        public int getNumberFlips();
    }

    static class AtomicIntegerCoinFlipper implements CoinFlipper {

        private AtomicInteger numberHeads = new AtomicInteger();
        private AtomicInteger numberFlips = new AtomicInteger();

        @Override
        public void flip(boolean heads) {
            numberFlips.incrementAndGet();
            if (heads) {
                numberHeads.incrementAndGet();
            }
        }

        @Override
        public int getNumberHeads() {
            return numberHeads.get();
        }

        @Override
        public int getNumberFlips() {
            return numberFlips.get();
        }
    }

    static class SynchronizedAccessCoinFlipper implements CoinFlipper {

        private int numberHeads = 0;
        private int numberFlips = 0;

        @Override
        public void flip(boolean heads) {
            synchronized (this) {
                numberFlips++;
                if (heads) {
                    numberHeads++;
                }
            }
        }

        @Override
        public int getNumberHeads() {
            synchronized (this) {
                return numberHeads;
            }
        }

        @Override
        public int getNumberFlips() {
            synchronized (this) {
                return numberFlips;
            }
        }
    }
    
    static class LockCoinFlipper implements CoinFlipper {
        private ReentrantLock lock = new ReentrantLock();
        private int numberHeads = 0;
        private int numberFlips = 0;
        
        @Override
        public void flip(boolean heads) {
            lock.lock();
            try {
                numberFlips++;
                if(heads) numberHeads++;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int getNumberHeads() {
            lock.lock();
            try {
                return numberHeads;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int getNumberFlips() {
            lock.lock();
            try {
                return numberFlips;
            } finally {
                lock.unlock();
            }
        }
        
    }

    static class CoinFlipperTask implements Runnable {

        private CoinFlipper coin;
        private int iterations;
        private Random random;

        public CoinFlipperTask(CoinFlipper coin, int iterations, Random r) {
            this.coin = coin;
            this.iterations = iterations;
            this.random = r;
        }

        public void run() {
            for (int i = 0; i < iterations; i++) {
                coin.flip(random.nextBoolean());
            }
        }
    }
    
    static enum CoinFlipperType {
        ATOMIC_INTEGER, SYNCHRONIZED_BLOCK, REENTRANT_LOCK; 
    }
    
    static enum ProgramMode {
        NORMAL, SPEEDUP, SCALEUP;
    }

    public static void main(String[] args) throws InterruptedException {
        Map<Object, String> margs = mapifyArgs(args);
        CoinFlipperType type = CoinFlipperType.ATOMIC_INTEGER;
        ProgramMode mode = ProgramMode.NORMAL;
        
        int numberThreads = Integer.parseInt(args[args.length - 2]);
        if(numberThreads < 1) {
            throw new IllegalArgumentException("numberThreads < 1 : " + numberThreads);
        }
        int numberIterations = Integer.parseInt(args[args.length - 1]);
        if(numberIterations < 1) {
            throw new IllegalArgumentException("numberIterations < 1 : " + numberIterations);
        }
        
        if(margs.get("s") != null) {
            type = CoinFlipperType.SYNCHRONIZED_BLOCK;
        } else if(margs.get("l") != null) {
            type = CoinFlipperType.REENTRANT_LOCK;
        }
        
        if(margs.get("speedup") != null) {
            for(int i = 1; i < numberThreads; i++) {
                CoinFlip app = new CoinFlip(i, numberIterations, type);
                long t1 = System.currentTimeMillis();
                app.runTest();
                long elapsed = System.currentTimeMillis() - t1;
                System.out.printf("Heads: %d Flips: %d Threads: %d Time: %d ms\n", app.coinFlipper.getNumberHeads(), app.coinFlipper.getNumberFlips(), i, elapsed);
            }
        } else if(margs.get("scaleup") != null) {
            for(int i = 1; i < numberThreads; i++) {
                CoinFlip app = new CoinFlip(i, i*numberIterations, type);
                long t1 = System.currentTimeMillis();
                app.runTest();
                long elapsed = System.currentTimeMillis() - t1;
                System.out.printf("Heads: %d Flips: %d Threads: %d Time: %d ms\n", app.coinFlipper.getNumberHeads(), app.coinFlipper.getNumberFlips(), i, elapsed);
            }            
        } else {
            CoinFlip app;
            try {
                app = new CoinFlip(args);
            } catch (IllegalArgumentException ex) {
                printUsage();
                return;
            }

            // timed test
            long t1 = System.currentTimeMillis();
            app.runTest();
            long elapsed = System.currentTimeMillis() - t1;

            // print results
            System.out.printf("%d heads in %d coin tosses\n", app.coinFlipper.getNumberHeads(), app.coinFlipper.getNumberFlips());
            System.out.printf("Elapsed time: %dms\n", elapsed);
        }
    }
    
    private CoinFlipper coinFlipper;
    private Random random;
    private int numberIterations;
    private int numberThreads;
    private Thread threads[];

    public CoinFlip(String args[]) {
        if (!(args.length == 2 || (args.length == 3 && args[0].matches("-s|-l")))) {
            throw new IllegalArgumentException();
        }
        numberThreads = Integer.parseInt(args[args.length - 2]);
        if(numberThreads < 1) {
            throw new IllegalArgumentException("numberThreads < 1 : " + numberThreads);
        }
        numberIterations = Integer.parseInt(args[args.length - 1]);
        if(numberIterations < 1) {
            throw new IllegalArgumentException("numberIterations < 1 : " + numberIterations);
        }
        if ("-s".equals(args[0])) {
            coinFlipper = new SynchronizedAccessCoinFlipper();
        } else if("-l".equals(args[0])) {
            coinFlipper = new LockCoinFlipper();
        } else {
            coinFlipper = new AtomicIntegerCoinFlipper();
        }
        threads = new Thread[numberThreads];
        random = new Random();
    }
    
    public CoinFlip(int numberThreads, int numberIterations, CoinFlipperType type) {
        if(numberThreads < 1) {
            throw new IllegalArgumentException("numberThreads < 1 : " + numberThreads);
        }
        if(numberIterations < 1) {
            throw new IllegalArgumentException("numberIterations < 1 : " + numberIterations);
        }
        this.numberThreads = numberThreads;
        this.numberIterations = numberIterations;
        if(type == CoinFlipperType.REENTRANT_LOCK) {
            this.coinFlipper = new LockCoinFlipper();
        } else if(type == CoinFlipperType.SYNCHRONIZED_BLOCK) {
            this.coinFlipper = new SynchronizedAccessCoinFlipper();
        } else {
            this.coinFlipper = new AtomicIntegerCoinFlipper();
        }
        threads = new Thread[numberThreads];
        random = new Random();
    }

    public void runTest() throws InterruptedException {
        startTasks();
        // run any left over in the main thread
        new CoinFlipperTask(coinFlipper, numberIterations % numberThreads, random).run();
        awaitTaskCompletion();
    }

    private void awaitTaskCompletion() throws InterruptedException {
        // wait for threads to complete
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }

    private void startTasks() {
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new CoinFlipperTask(coinFlipper, numberIterations / numberThreads, random));
            threads[i].start();
        }
    }
    
    public void testSpeedup() throws InterruptedException {
        // save number of threads as the maximum
        int maxNumberThreads = numberThreads;
        for(int i = 1; i < maxNumberThreads; i++) {
            setNumberThreads(numberThreads);
            long t1 = System.currentTimeMillis();
            runTest();
            long elapsed = System.currentTimeMillis() - t1;
            System.out.printf("Heads: %d Flips: %d Threads: %d Time: %d ms\n", coinFlipper.getNumberHeads(), coinFlipper.getNumberFlips(), i, elapsed);
        }
    }
    
    public void testScaleup() throws InterruptedException {
        // save number of threads as the maximum
        int maxNumberThreads = numberThreads;
        int iterations = numberIterations;
        for(int i = 1; i < maxNumberThreads; i++) {
            setNumberThreads(numberThreads);
            long t1 = System.currentTimeMillis();
            runTest();
            long elapsed = System.currentTimeMillis() - t1;
            System.out.printf("Heads: %d Flips: %d Threads: %d Time: %d ms\n", coinFlipper.getNumberHeads(), coinFlipper.getNumberFlips(), i, elapsed);
        }
    }
    
    private void setNumberThreads(int numberThreads) {
        this.numberThreads = numberThreads;
        this.threads = new Thread[numberThreads];
    }
    
    /**
     * Turns an argument list into a map of key value pairs. One set of keys are 
     * the positions in the argument list. Eg, given the input {"asdf","qwerty"}
     * the output map will contain the entries {0:"asdf", 1:"qwerty"}
     * Additionally, argument of the form -arg will be inserted as keys 
     * associated with a non-null value and arguments of the form -arg=value 
     * will be inserted as key/value pairs. Eg given the input {"-q", "-a=10"}
     * the output map will contain the entries {"q":"q", "a":"10"}
     * @param args
     * @return 
     */
    private static Map<Object,String> mapifyArgs(String[] args) {
        Map<Object,String> output = new HashMap<Object, String>(2 * args.length);
        Pattern argPattern = Pattern.compile("-([^=]*)(?:=(.*))?");
        for(int i = 0; i < args.length; i++) {
            output.put(i, args[i]);
            Matcher m = argPattern.matcher(args[i]);
            if(m.matches()) {
                if(m.group(2) != null) {
                    output.put(m.group(1), m.group(2));
                } else {
                    output.put(m.group(1), m.group(1));
                }
            }
        }
        return output;
    }
    
    
}