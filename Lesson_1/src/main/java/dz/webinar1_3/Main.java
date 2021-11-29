package dz.webinar1_3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
1. Написать метод, который меняет два элемента массива местами.(массив может быть любого ссылочного типа);
2. Написать метод, который преобразует массив в ArrayList;
3. Большая задача:

a. Есть классы Fruit -> Apple, Orange;(больше фруктов не надо)
b. Класс Box в который можно складывать фрукты, коробки условно сортируются по типу фрукта, поэтому в одну коробку
    нельзя сложить и яблоки, и апельсины;
c. Для хранения фруктов внутри коробки можете использовать ArrayList;
d. Сделать метод getWeight() который высчитывает вес коробки, зная количество фруктов и вес одного фрукта(вес
    яблока - 1.0f, апельсина - 1.5f, не важно в каких это единицах);
e. Внутри класса коробка сделать метод compare, который позволяет сравнить текущую коробку с той, которую подадут
    в compare в качестве параметра, true - если их веса равны, false в противном случае(коробки с яблоками мы
    можем сравнивать с коробками с апельсинами);
f. Написать метод, который позволяет пересыпать фрукты из текущей коробки в другую коробку(помним про сортировку
    фруктов, нельзя яблоки высыпать в коробку с апельсинами), соответственно в текущей коробке фруктов
    не остается, а в другую перекидываются объекты, которые были в этой коробке;
g. Не забываем про метод добавления фрукта в коробку.

 */
public class Main {
    public static void main(String[] args) {
        /********************   1-е задание   ********************/
        Integer [] test1 = {1, 2, 3, 4, 5, 6};
        swapElements(test1, 1, 4);
        System.out.println(Arrays.asList(test1));

        String [] test2 = {"11", "22", "33", "44", "55", "66"};
        swapElements(test2, 3, 4);
        System.out.println(Arrays.asList(test2));

        /********************   2-е задание   ********************/
        String [] test = {"First", "Second", "Third", "Fourth", "Fifth", "Sixth"};
        System.out.println("Массив: " + test.getClass().getSimpleName());
        System.out.println("Лист: " + (asArrayList(test)).getClass().getSimpleName());

        /********************   3-е задание   ********************/
        Box box = new Box();
        Box box2 = new Box();
        Apple apple = new Apple();
        Orange orange = new Orange();

        System.out.println();
        System.out.println(box.put(apple)); //true - успешно положили в пустую коробку
        System.out.println(box.put(orange)); //false - апельсины не сочетаются с яблоками
        System.out.println(box.put(apple, 83)); //true
        System.out.println(box.put(orange, 22)); //false
        System.out.println(box.getWeight());
        System.out.println(); //Пустая строка

        box2.put(orange, 56);
        System.out.println(box2.getWeight());
        System.out.println(box2.compare(box));
        System.out.println();

        System.out.println(box.moveFruitsTo(box2)); //false - нельзя смешивать яблоки с апельсинами
        Box box3 = new Box();
        System.out.println(box3.getWeight());
        System.out.println(box.moveFruitsTo(box3));
        System.out.println(box3.getWeight());
        System.out.println(box3.getElementsType());

        //Не нашёл, где тут можно применить компаратор, т.к. задания на больше-меньше нет
    }

    public static <T> void swapElements (T[] arr, int first, int second) {
        T tempElement = arr [first];
        arr [first] = arr [second];
        arr [second] = tempElement;
    }

    public static <T> List<T> asArrayList (T [] arr) {
        return Arrays.asList(arr);
    }
}
