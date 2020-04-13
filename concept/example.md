##Concept

####Basic API
```scala
object BasicApp extends App {
  
  val a = List(1,2,3)
  
  val crossMap = GMap {
  
  } 
  
  val croffFilter = GFilter {
  
  }

  val progmap = mem.transform(crossMap)
  val progWithFilter = progmap.tranform(crossFilter)

  val mem = mem.transform(crossMap).tranform(crossFilter).perform()
  
  val mapAndFilter = crossMap.andThen(corssFilter)
   
  val (last, all) = planets.mapAggregate(nBody, 100)
  

  result.foreach(storeToFile("dsa"))

  mem.tranfsormN(nBody, 10)
  mem.saveToFIle("dsa")
  
}
```