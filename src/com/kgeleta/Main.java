package com.kgeleta;


import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        int x = 10, y = 12, flag;

        if(x < y)       // Vx < Vy
        {
            flag = 0;// borrow
            x = (256 - (y - x)) & 0x000000FF;
        }
        else {
            flag = 1;
            x = (x - y) & 0x000000FF;
        }


        System.out.println("x = " + x + " y = " + y);
//        System.out.println(String.format("0x%08X",chip8.getMemory()[556] & 0x00FF));


    }
}
