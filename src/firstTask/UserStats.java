package firstTask;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by user on 12/15/14.
 */
public class UserStats {
    public Map<Integer, Integer> userStats = new HashMap<>();
    public Map<Integer, Integer> userLoginTimes = new HashMap<>();
    public int READ_BUFFER_LENGTH = 1000;

    public static void main(String[] args) throws IOException {
        UserStats userStats = new UserStats();
        long start = System.currentTimeMillis();
        userStats.readFile(args[0]);
        System.out.println(System.currentTimeMillis() - start);
        userStats.printStats();
    }

    public void readFile(String path) throws IOException {
        FileInputStream in = new FileInputStream(path);
        FileChannel channel = in.getChannel();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

        byte[] barray = new byte[READ_BUFFER_LENGTH];
        int nGet;
        String tail = "";
        while (buffer.hasRemaining()) {
            nGet = Math.min(buffer.remaining(), READ_BUFFER_LENGTH);
            buffer.get(barray, 0, nGet);
            String[] lines = new String(barray).substring(0, nGet).split("\n");
            lines[0] = tail + lines[0];

            for (int i = 0; i < lines.length - 1; i++){
                processLine(lines[i]);
            }

            tail = lines[lines.length - 1];
        }
        processLine(tail);

    }

    public void processLine(String line) {
        if (line == null) {
            return;
        }

        String[] split = line.split(", ");
        Integer userId = Integer.parseInt(split[1]);
        Integer time = Integer.parseInt(split[0]);

        if (split[2].equals("login")) {
            userLoginTimes.put(userId, time);
            if (!userStats.containsKey(userId)) {
                userStats.put(userId, 0);
            }
        } else {
            Integer loginTime = userLoginTimes.remove(userId);
            userStats.put(userId, userStats.get(userId) + time - loginTime);
        }
    }

    public void printStats() {
        List<Map.Entry<Integer,Integer>> userIdFullTimeEntities = new ArrayList<>();
        userIdFullTimeEntities.addAll(userStats.entrySet());
        Collections.sort(userIdFullTimeEntities, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> integerIntegerEntry, Map.Entry<Integer, Integer> integerIntegerEntry2) {
                return -Integer.compare(integerIntegerEntry.getValue(), integerIntegerEntry2.getValue());
            }
        });

        for (Map.Entry<Integer, Integer> entry:userIdFullTimeEntities) {
            Integer userId = entry.getKey();
            Integer totalTime = entry.getValue();
            int day = (int) TimeUnit.SECONDS.toDays(totalTime);
            long hours = TimeUnit.SECONDS.toHours(totalTime) - (day *24);
            long minute = TimeUnit.SECONDS.toMinutes(totalTime) - (TimeUnit.SECONDS.toHours(totalTime)* 60);
            long second = TimeUnit.SECONDS.toSeconds(totalTime) - (TimeUnit.SECONDS.toMinutes(totalTime) *60);
            System.out.println(String.format("User id: %s Total online time: %s days %s hours %s minuts %s seconds", userId, day, hours, minute, second));
        }
    }
}
