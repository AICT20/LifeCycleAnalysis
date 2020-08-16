package soot.jimple.infoflow.pattern.patternresource;

public enum LCResourceOPType {
    REQUIRE, RELEASE, FINISH, //这个是针对Pattern1的
    SAVE, EDIT, //这个是针对Pattern2的, , SETFIELD, GETFIELD不再使用
    SP_SET, SP_GET,
    UNKNOWN,
}
