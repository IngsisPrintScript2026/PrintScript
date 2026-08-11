package node;

import java.util.List;

//Composite an PODO (Plain Old Data Object)
public interface Node {
    Integer line();
    Integer column();
    String symbol();
    List<? extends Node> children();
}
