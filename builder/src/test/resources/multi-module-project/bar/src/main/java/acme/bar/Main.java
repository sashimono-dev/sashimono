package acme.bar;

import acme.foo.Greeter;
public class Main {
    public static void main(String ... args) {
        System.out.println(new Greeter().greet());
    }

}
