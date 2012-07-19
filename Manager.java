
import java.util.List;

@MongoCollection
class Manager extends Employee {
	@MongoField String name;
    @MongoField int parkingSpot;
    @MongoField List<Employee> directReports;
}