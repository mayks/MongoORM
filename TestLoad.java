import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.List;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;


public class TestLoad {
	@SuppressWarnings("unused")
	public static void main(String args[]) throws UnknownHostException, MongoException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException
	{
		Mongo m = new Mongo();
		DB db = m.getDB("orm");
		MongoORM orm = new MongoORM(db);
		
		List<Employee> employee = orm.loadAll(Employee.class);
		List<Project> project = orm.loadAll(Project.class);
		List<Manager> manager = orm.loadAll(Manager.class);
		System.out.println();
	}
}
