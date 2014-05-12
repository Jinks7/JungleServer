package chat.server;

import java.io.*;

// make it so passwords are invisible on the command line
class EraserThread implements Runnable {
   private boolean stop;

   /**
    *@REMOVED The prompt displayed to the user 
    */ 
   public EraserThread() {
       
   }

   /**
    * Begin masking...display asterisks (*)
    */
   public void run () {
      stop = true;
      while (stop) {
         System.out.print("\010 ");
     try {
        Thread.currentThread().sleep(1);
         } catch(InterruptedException ie) {
            ie.printStackTrace();
         }
      }
   }

   /**
    * Instruct the thread to stop masking
    */
   public void stopMasking() {
      this.stop = false;
   }
}