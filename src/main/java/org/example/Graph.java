package org.example;

import java.util.*;

public class Graph {

    public static void main(String[] args) {



    }

    public void chessTask(){
        Scanner scanner = new Scanner(System.in);
        String[] nm = scanner.nextLine().split(" ");
        int n = Integer.parseInt(nm[0]);
        int m = Integer.parseInt(nm[1]);
        List<List<Integer>> list = new ArrayList<List<Integer>>();
        for (int i = 0; i < n; i++){
            list.add(new ArrayList<>());
        }
        HashSet<Integer> winSet = new HashSet<>();
        int[] map = new int[n + 1];
        for (int i = 1; i <= n; i++){
            winSet.add(i);
        }
        for (int i = 0; i < m; i++){
            String[] line = scanner.nextLine().split(" ");
            int f = Integer.parseInt(line[0]);
            int s = Integer.parseInt(line[1]);
            int win = Integer.parseInt(line[2]);
            if (win == 1){
                list.get(f - 1).add(s);
                winSet.remove(s);
            } else {
                list.get(s - 1).add(f);
                winSet.remove(f);
            }

        }
        int lastIndex = -1;
        if (winSet.size() == 1){
            int win = winSet.iterator().next();
            map[win] = n + 1;
            Stack<Integer> stack = new Stack<>();
            stack.add(win);
            while (!stack.isEmpty()){
                int value = stack.pop();
                if (list.get(value - 1).size() == 0) lastIndex = value;
                for (int i = 0; i < list.get(value - 1).size(); i++) {
                    int item = list.get(value - 1).get(i);
                    int a = map[item];
                    if (a == 0) {
                        stack.add(item);
                    }
                    map[item] = value;
                }
            }

            if (lastIndex == -1){
                System.out.print("NO");
            } else {
                if (r(map, lastIndex, n) == 1) {
                    int sum = 0;
                    for (int i: map){
                        sum += i;
                    }
                    if (sum == (n * (n- 1)) / 2 + n + 1){
                        System.out.print("YES");
                    } else {
                        System.out.print("NO");
                    }
                } else {
                    System.out.print("NO");
                }

            }
        } else {
            System.out.print("NO");
        }
    }



    public static int r(int[] a, int b, int n) {
        if (a[b] == n + 1) return n;
        a[b] = r(a, a[b], n) - 1;
        return a[b];
    }


    public void matrix(){
        Scanner scanner = new Scanner(System.in);
        String[] nm = scanner.nextLine().split(" ");
        int n = Integer.parseInt(nm[0]);
        int m = Integer.parseInt(nm[1]);
        int[][] array = new int[n][n];
        int[][] array2 = new int[n][n];
        for (int i = 0; i < m; i++) {
            String[] k_ = scanner.nextLine().split(" ");
            int k0 = Integer.parseInt(k_[0]);
            if (k0 != 1) {
                for (int j = 2; j <= k0; j++) {
                    int prev = Integer.parseInt(k_[j - 1]);
                    array[prev - 1][Integer.parseInt(k_[j]) - 1] = 1;
                    array[Integer.parseInt(k_[j]) - 1][prev - 1] = 1;
                }
            }

            for (int j = 1; j <= k0; j++) {
                for (int h = 1; h <= k0; h++) {
                    if (h != j) {
                        int item = Integer.parseInt(k_[h]) - 1;
                        array2[Integer.parseInt(k_[j]) - 1][item] = 1;
                    }
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print(array[i][j]);
                if (j != n - 1) {
                    System.out.print(" ");
                } else {
                    System.out.print("\n");
                }
            }

        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                System.out.print(array2[i][j]);
                if (j != n - 1) {
                    System.out.print(" ");
                } else {
                    System.out.print("\n");
                }
            }

        }

    }
}

