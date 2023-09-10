package org.example.javaDeveloper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class Tasks {

    public static void main(String[] args) throws InterruptedException {

    }

    public static void first(){
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        int number = Integer.parseInt(input);

        if (number == 1) {
            System.out.println(1);
        } else {
            System.out.println(2 * (2 * number - 2));
        }
    }

    private static void third(){
        Scanner scanner = new Scanner(System.in);
        String[] input = scanner.nextLine().split(" ");
        int h = Integer.parseInt(input[1]);
        int w = Integer.parseInt(input[0]);

        List<int[]> list = new ArrayList<>(h);

        int firstRow = 2;
        for (int i = 0; i < h; i++) {
            if (i <= h - 1) {
                if (i == 0) firstRow = 1;
                else firstRow = firstRow + i + 1;
            } else {
                firstRow += h;
            }
            int[] column = new int[w];
            column[0] = firstRow;
            list.add(column);
        }


        for (int h_ = 0; h_ < h; h_++) {
            int[] column = list.get(h_);
            for (int w_ = 1; w_ < w; w_++) {
                int prev = column[w_ - 1];

                if (w_ <= h - 1 && h_ < h - 1) {
                    column[w_] = prev + w_ + h_;
                } else if (w_ > w - h && h - h_ >= 1) {
                    column[w_] = prev + w - w_ - 1 + h - h_ - 2;
                } else {
                    column[w_] = prev + h;
                }

            }
            list.set(h_, column);
        }
        for (int i = 0; i < h; i++) {
            System.out.println(Arrays.toString(list.get(i)));
        }


    }
}
