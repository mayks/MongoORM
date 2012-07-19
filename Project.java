import java.util.Date;

@MongoCollection
class Project {
    @MongoField String name;
    @MongoField Date begin;
    @MongoField Date end;
}