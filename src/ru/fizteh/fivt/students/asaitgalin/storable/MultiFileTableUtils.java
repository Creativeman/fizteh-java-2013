package ru.fizteh.fivt.students.asaitgalin.storable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiFileTableUtils {
    public static List<Class<?>> getColumnTypes(String[] values) {
        List<Class<?>> columnsList = new ArrayList<>();
        for (String s : values) {
            switch (s) {
                case "int":
                    columnsList.add(Integer.class);
                    break;
                case "long":
                    columnsList.add(Long.class);
                    break;
                case "byte":
                    columnsList.add(Byte.class);
                    break;
                case "float":
                    columnsList.add(Float.class);
                    break;
                case "double":
                    columnsList.add(Double.class);
                    break;
                case "boolean":
                    columnsList.add(Boolean.class);
                    break;
                case "String":
                    columnsList.add(String.class);
                    break;
                default:
                    columnsList.add(null);
                    break;
            }
        }
        return columnsList;
    }

    public static String getColumnTypeString(Class<?> columnType) {
        switch (columnType.getSimpleName()) {
            case "Integer":
                return "int";
            case "Long":
                return "long";
            case "Byte":
                return "byte";
            case "Float":
                return "float";
            case "Double":
                return "double";
            case "Boolean":
                return "boolean";
            case "String":
                return "String";
            default:
                return null;
        }
    }

    public static boolean isValidType(String type) {
        Set<String> validTypes = new HashSet<>();
        validTypes.add("int");
        validTypes.add("long");
        validTypes.add("byte");
        validTypes.add("float");
        validTypes.add("double");
        validTypes.add("boolean");
        validTypes.add("String");
        return validTypes.contains(type);
    }
}
