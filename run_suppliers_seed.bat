@echo off
"C:\Program Files\Java\jdk-17\bin\java.exe" -Dfile.encoding=UTF-8 -cp "C:\Users\rezau\eclipse-workspace\ny-coffee-pos\target\classes;C:\Users\rezau\eclipse-workspace\ny-coffee-pos\lib\mysql-connector-j-9.7.0\mysql-connector-j-9.7.0\mysql-connector-j-9.7.0.jar" com.possystem.tools.SeedSuppliers > "C:\Users\rezau\eclipse-workspace\ny-coffee-pos\seed_suppliers_log.txt" 2>&1
echo DONE_MARKER >> "C:\Users\rezau\eclipse-workspace\ny-coffee-pos\seed_suppliers_log.txt"
