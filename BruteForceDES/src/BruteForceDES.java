import java.util.Random;
import javax.crypto.SealedObject;

/**
 *
 * @author dgc30
 */
public class BruteForceDES {
    /**
     * Modification of SealedDES main function that searches for the key in 
     * parallel.
     */
    public static void main(String[] args) throws InterruptedException {
        if(!(args.length == 2 || (args.length == 3 && args[0].matches("-speedup|-scaleup")))) {
            System.out.println("Usage: java BruteForceDES [-scaleup|-speedup] t s");
            System.out.println(" t number of threads control");
            System.out.println(" s key size in bits");
            System.out.println(" -speedup measure speedup by running successively with 1..t threads");
            System.out.println(" -scaleup measure scaleup by running successively with 1..t threads");            
            System.out.println("          and key space s..t*s");            
            return;
        }

        // get the number of threads to use
        int numThreads = Integer.parseInt(args[args.length - 2]);
        
        // Get the argument
        int keybits = Integer.parseInt(args[args.length - 1]);

        if(args.length == 3 && args[0].equals("-speedup")) {
            for(int i = 1; i <= numThreads; i++) {
                breakDes(keybits, i, false);            
            }
        } else if (args.length == 3 && args[0].equals("-scaleup")) {            
            long baseRange = (0xFFFFFFFFFFFFFFFFL >>> (64 - keybits)) + 1;
            for(int i = 1; i <= numThreads; i++) {
                long maxkey = baseRange * i;
                // Accurate up to 52 bits of key
                long key = (long) (Math.random() * maxkey);
                breakDes(key, maxkey - 1, i, false);
            }            
        } else {
            breakDes(keybits, numThreads, true);
        }
    }

    private static void breakDes(int keybits, int numThreads, boolean printStatus) throws InterruptedException {
        long maxkey = 0xFFFFFFFFFFFFFFFFL >>> (64 - keybits);

        // Get a random key within the given range
        long key = new Random().nextLong() & maxkey;

        breakDes(key, maxkey, numThreads, printStatus);
    }

    private static void breakDes(long key, long maxkey, int numThreads, boolean printStatus) throws InterruptedException {
        // Create a simple cipher
        SealedDES enccipher = new SealedDES();

        // Set up a key
        enccipher.setKey(key);

        // Generate a sample string
        String plainstr = "Johns Hopkins afraid of the big bad wolf?";

        // Here ends the set-up.  Pretending like we know nothing
        // discover what key was used to encrypt the message.

        // Get and store the current time -- for timing
        long runstart = System.currentTimeMillis();

        // start threads
        Thread[] threads = new Thread[numThreads];
        for(int i = 0; i < numThreads; i++) {
            long startKey = i * maxkey/numThreads;
            // last thread will seach a little more of the key space to ensure every key is searched
            long stopKey = (i == numThreads - 1) ? maxkey + 1 : startKey + maxkey/numThreads;            
            threads[i] = new Thread(new SearchKeysTask(enccipher.encrypt(plainstr), "Hopkins", startKey, stopKey, runstart, printStatus), "Thread " + i);
            threads[i].start();
        }
        
        // wait for threads to complete
        for(int i = 0; i < numThreads; i++) {
            threads[i].join();
        }

        // Output search time
        long elapsed = System.currentTimeMillis() - runstart;
        System.out.printf("Completed search of %d keys at %d ms using %d threads.\n", maxkey + 1, elapsed, numThreads);
    }

    static class SearchKeysTask implements Runnable {
        
        private final SealedDES sealedDes;
        private final SealedObject sealedObject;
        private final String searchString;
        private final long startKey;
        private final long stopKey;
        private final long runstart;
        private final boolean printStatus;
        // null indicates key not found
        private Long foundKey;

        public SearchKeysTask(SealedObject sealedObject, String searchString, long startKey, long stopKey, long runstart, boolean printStatus) {
            this.sealedObject = sealedObject;
            this.searchString = searchString;
            this.startKey = startKey;
            this.stopKey = stopKey;
            this.runstart = runstart;
            this.printStatus = printStatus;
            this.foundKey = null;
            // Create a simple cipher
            this.sealedDes = new SealedDES();
        }

        @Override
        public void run() {
            // Search for the right key
            for (long i = startKey; i < stopKey; i++) {
                // Set the key and decipher the object
                sealedDes.setKey(i);
                String decryptstr = sealedDes.decrypt(sealedObject);

                // Does the object contain the known plaintext
                if ((decryptstr != null) && (decryptstr.indexOf(searchString) != -1)) {
                    if(printStatus) {
                        System.out.printf("%s Found decrypt key %016x producing message: %s\n", Thread.currentThread().getName(), i, decryptstr);
                    }
                    this.foundKey = i;
                }

                // Update progress every once in awhile.
                //  Remove printlns if running for time.
                if (printStatus && i % 100000 == 0) {
                    System.out.printf("%s Searched key number %d at %d milliseconds.\n", Thread.currentThread().getName(), i, System.currentTimeMillis() - runstart);
                }
            }
        }
        
        public Long getFoundKey() {
            return foundKey;
        }
    }
}
