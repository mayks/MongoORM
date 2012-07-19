
import java.util.List;

@MongoCollection("empl")
class Employee {
    @MongoField String name;
    @MongoField("salary") double yearlySalary;
    @MongoField Employee manager;
    @MongoField List<Project> projects;
    int ignoredField;
}
