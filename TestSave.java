import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class TestSave {
	public static void main(String args[]) throws UnknownHostException, MongoException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException
	{
		Mongo m = new Mongo();
		DB db = m.getDB("orm");
		MongoORM orm = new MongoORM(db);
		db.dropDatabase();
		
		Project project1 = new Project();
		project1.name = "project1";
		project1.begin = new Date();
		orm.save(project1);
		
		Project project2 = new Project();
		project2.name = "project2";
		project2.begin = new Date();
		project2.end = new Date();
		
		Manager tombu = new Manager();
		tombu.name = "tombu";
		Employee parrt = new Employee();
		parrt.name = "parrt";
		
		tombu.parkingSpot = 2;
		tombu.yearlySalary = (float) 200;
		tombu.directReports = new ArrayList<Employee>();
		tombu.directReports.add(parrt);
		
		parrt.yearlySalary = (float) 100.5;
		parrt.projects = new ArrayList<Project>();
		parrt.projects.add(project1);
		parrt.projects.add(project2);
		parrt.manager = tombu;
		
		orm.save(tombu);
		orm.save(parrt);
		orm.save(project2);
	}
}
