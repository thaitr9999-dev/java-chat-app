package com.kimtruong.chat_app;

public class Temp {
    private static String color [] = {"red","blue","green","yellow","puple"};
    private static int [] selections = new int [color.length ] ; 
    private static int [] vaidValue  = {0,1} ;

    private static void backtracking(int currIndex){
        for(int i = 0 ; i < vaidValue.length; i ++){
            selections[currIndex] = vaidValue[i] ; 
            if(currIndex == color.length - 1 ){
                process(selections);
            }  else {
                backtracking(currIndex + 1);
            }
        }


    }
    private static  int sum = 0 ; 
    private static void process(int [] selections){
        String newGenColor = "" ; 
        for(int i = 0 ; i < selections.length; i ++){
            if(selections[i] == 1){
                newGenColor += color[i] + " " ; 
            }
           
        }
        sum ++ ; 
        System.out.println(sum + " " + newGenColor);


    }


    public static void main(String[] args) {
        backtracking(0);

    }
}