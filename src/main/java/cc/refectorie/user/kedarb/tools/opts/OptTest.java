package cc.refectorie.user.kedarb.tools.opts;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OptTest {
    @Opt(gloss = "boolean field", required = true)
    public boolean b = false;

    @Opt
    public String s = "";

    @Opt
    public int i = 0;

    @Opt
    public double d = 1.0;

    @Opt(gloss = "enum field")
    public TestEnum e = TestEnum.a;

    @Opt(gloss = "array field")
    public String[] sarr = new String[]{};

    @Opt(gloss = "really long option")
    public int lllllllllllllyLooooooongggggOption = -1;


    @Opt
    public BufferedReader reader = new BufferedReader(new InputStreamReader(
            System.in));

    public String notAnOption = "ha!";

    public enum TestEnum {
        a, b, c
    }

    public static void main(String args[]) {
        OptTest opts = new OptTest();
        OptParser parser = new OptParser(opts);
        if (!parser.doParse(args))
            System.exit(1);
        // do processing as normal, fields will be automatically filled
    }
}
