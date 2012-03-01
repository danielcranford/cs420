
import java.io.PrintStream;
import java.util.Random;
import javax.crypto.SealedObject;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
        if (args.length != 2) {
            System.out.println("Usage: java BruteForceDES t s");
            System.out.println(" t number of threads");
            System.out.println(" s key size in bits");
            return;
        }

        // get the number of threads to use
        final int numThreads = Integer.parseInt(args[0]);
        
        // Get the argument
        long keybits = Long.parseLong(args[1]);

        final long maxkey = 0xFFFFFFFFFFFFFFFFL >>> (64 - keybits);

        // Create a simple cipher
        final SealedDES enccipher = new SealedDES();

        // Get a number between 0 and 2^64 - 1
        long key = new Random().nextLong();

        // Mask off the high bits so we get a short key
        key = key & maxkey;

        // Set up a key
        enccipher.setKey(key);

        // Generate a sample string
        final String plainstr = "Johns Hopkins afraid of the big bad wolf?";

        // Encrypt
        SealedObject sldObj = enccipher.encrypt(plainstr);

        // Here ends the set-up.  Pretending like we know nothing except sldObj,
        // discover what key was used to encrypt the message.

        // Get and store the current time -- for timing
        final long runstart = System.currentTimeMillis();

        // start threads
        Thread[] threads = new Thread[numThreads];
        for(int i = 0; i < numThreads; i++) {
            final long startKey = i * maxkey/numThreads;
            // last thread will seach a little more of the key space to ensure every key is searched
            final long stopKey = (i == numThreads - 1) ? maxkey : startKey + maxkey/numThreads;
            threads[i] = new Thread(new Runnable(){
                // Create a simple cipher
                SealedDES deccipher = new SealedDES();
                SealedObject sldObj = enccipher.encrypt(plainstr);

                @Override
                public void run() {
                    // Search for the right key
                    for (long i = startKey; i < stopKey; i++) {
                        // Set the key and decipher the object
                        deccipher.setKey(i);
                        String decryptstr = deccipher.decrypt(sldObj);

                        // Does the object contain the known plaintext
                        if ((decryptstr != null) && (decryptstr.indexOf("Hopkins") != -1)) {
                            //  Remove printlns if running for time.
                            System.out.printf(Thread.currentThread().getName() + " Found decrypt key %016x producing message: %s\n", i, decryptstr);
                        }

                        // Update progress every once in awhile.
                        //  Remove printlns if running for time.
                        if (i % 100000 == 0) {
                            long elapsed = System.currentTimeMillis() - runstart;
                            System.out.println(Thread.currentThread().getName() + " Searched key number " + i + " at " + elapsed + " milliseconds.");
                        }
                    }
                }
            }, "Thread " + i);
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
}
