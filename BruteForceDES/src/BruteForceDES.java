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
            System.out.println(" t number of threads");
            System.out.println(" s key size in bits");
            return;
        }

        // get the number of threads to use
        int numThreads = Integer.parseInt(args[args.length - 2]);
        
        // Get the argument
        long keybits = Long.parseLong(args[args.length - 1]);

        long maxkey = 0xFFFFFFFFFFFFFFFFL >>> (64 - keybits);

        // Create a simple cipher
        SealedDES enccipher = new SealedDES();

        // Get a random key within the given range
        long key = new Random().nextLong() & maxkey;

        // Set up a key
        enccipher.setKey(key);

        // Generate a sample string
        String plainstr = "Johns Hopkins afraid of the big bad wolf?";

        // Here ends the set-up.  Pretending like we know nothing except sldObj,
        // discover what key was used to encrypt the message.

        // Get and store the current time -- for timing
        long runstart = System.currentTimeMillis();

        // start threads
        Thread[] threads = new Thread[numThreads];
        for(int i = 0; i < numThreads; i++) {
            final long startKey = i * maxkey/numThreads;
            // last thread will seach a little more of the key space to ensure every key is searched
            final long stopKey = (i == numThreads - 1) ? maxkey : startKey + maxkey/numThreads;            
            threads[i] = new Thread(new SearchKeysTask(enccipher.encrypt(plainstr), "Hopkins", startKey, stopKey, runstart), "Thread " + i);
            threads[i].start();
        }
        
        // wait for threads to complete
        for(int i = 0; i < numThreads; i++) {
            threads[i].join();
        }

        // Output search time
        long elapsed = System.currentTimeMillis() - runstart;
        long keys = maxkey + 1;
        System.out.println("Completed search of " + keys + " keys at " + elapsed + " milliseconds.");
    }

    static class SearchKeysTask implements Runnable {
        
        private final SealedDES sealedDes;
        private final SealedObject sealedObject;
        private final String searchString;
        private final long startKey;
        private final long stopKey;
        private final long runstart;

        public SearchKeysTask(SealedObject sealedObject, String searchString, long startKey, long stopKey, long runstart) {
            this.sealedObject = sealedObject;
            this.searchString = searchString;
            this.startKey = startKey;
            this.stopKey = stopKey;
            this.runstart = runstart;
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
                    //  Remove printlns if running for time.
                    System.out.printf("%s Found decrypt key %016x producing message: %s\n", Thread.currentThread().getName(), i, decryptstr);
                }

                // Update progress every once in awhile.
                //  Remove printlns if running for time.
                if (i % 100000 == 0) {
                    System.out.printf("%s Searched key number %d at %d milliseconds.\n", Thread.currentThread().getName(), i, System.currentTimeMillis() - runstart);
                }
            }
        }
    }
}
