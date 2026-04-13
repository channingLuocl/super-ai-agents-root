import java.util.Scanner;

// 注意类名必须为 Main, 不要有任何 package xxx 信息
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int m = in.nextInt();
        // boolean[] load = new boolean[n];
        // ArrayList<ArrayList<Integer>> load =new ArrayList<>();
        int[] count = new int[n];
        for (int i = 0; i < m; i++) {
            int left = in.nextInt() - 1;
            int right = in.nextInt() - 1;
            for (int j = left; j <= right; j++) {
                count[j]++;
            }
        }
        int maxIndex = 0;
        int maxLen = count[0];
        for (int i = 1; i < n; i++) {
            if (count[i] > maxLen) {
                maxIndex = i;
                maxLen = Math.max(maxLen, count[i]);
            }
        }
        // int min = 0;
        // for (int i = 0; i < m; i++) {
        //     if (load[maxIndex][i])min += 1;
        //     else min += 2;
        // }
        System.out.println((2 * m - maxLen) + " " + (maxIndex + 1));
    }
}