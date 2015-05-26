package raft;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class Client {
    private static Map<String,String> our = new HashMap<>();
    private static String[] query = {"GET","SET","DELETE"};
    private static String response = "";
    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 8000);
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String q = generateQuery();

            s.getOutputStream().write(q.getBytes());
            System.out.println("now "+q);
            byte[] buf = new byte[1024];
            int r = s.getInputStream().read(buf);
            String serverResponse = new String(buf, 0, r);
            if (!response.equals(serverResponse)) {
                System.out.println("BAD" + response +" " + serverResponse);
            } else {
                System.out.println(serverResponse + " all OK ");
            }
        }
    }
    private static String generateQuery() {
        String ans = "";
        Random random = new Random();
        int num = random.nextInt(3);
        ans = query[num];
        int key = random.nextInt(20);
        if (num == 1) {/*SET*/
            int value = random.nextInt(100);
            our.put(Integer.toString(key), Integer.toString(value));
            ans += " "+ key +" " + value;
            response = "OK";
        } else {
            if (num == 2) {/*DELETE*/
                if (our.containsKey(key)) {
                    our.remove(key);
                    response = "OK";
                } else {
                    response = "NOT_FOUND";
                }
                ans += " " + key;
            } else {/*GET*/
                if (our.containsKey(key)) {
                    response = our.get(key);
                } else {
                    response = "NOT_FOUND";
                }
                ans += " " + key;
            }
        }
        return "client_request " + ans + " " + "0 ";
    }


}