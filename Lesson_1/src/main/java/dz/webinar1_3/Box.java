package dz.webinar1_3;

import java.util.ArrayList;

public class Box <T extends Fruit> {
    private T firstElement = null;
    private final ArrayList <Fruit> elements = new ArrayList <>();


    public boolean put (T fruit) {
        if (firstElement == null || fruit.getClass().getSimpleName().equals(firstElement.getClass().getSimpleName())) {
            elements.add(fruit);
            firstElement = (T) elements.get (0);
            return true; //Положили успешно в коробку
        }
        return false;
    }

    public boolean put (T fruit, int quantity) {
        if (quantity > 0) {
            for (int i = 0; i < quantity; i++) {
                if (!put(fruit)) {
                    return false;
                }
            }
        }
        return true;
    }

    public float getWeight () {
        if (elements.size() > 0) {
            return elements.size() * elements.get (0).getWeight();
        } else {
            return 0f;
        }
    }

    public String getElementsType () {
        if (firstElement != null) {
            return firstElement.getClass().getSimpleName();
        } else {
            return null;
        }
    }

    public boolean compare (Box box) {
        if (box != null) {
            if (Math.abs(getWeight() - box.getWeight()) < 0.01f) {
                //Если массы фруктов равны, то вернем true
                return true;
            }
        }
        return false;
    }

    public boolean moveFruitsTo (Box box) {
        if (box != null && firstElement != null) {
            //Если другая коробка существует, а также в нашей коробке что-то есть
            if (box.put (firstElement, elements.size())) {
                //Если удалось положить в другую коробку, то фрукты одинаковые
                elements.clear();
                firstElement = null;
                return true;
            }
        }
        return false;
    }
}
