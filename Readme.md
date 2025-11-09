prodcons/
├─ options.xml
├─ v1/ //  mvn -q clean package mvn -q exec:java -Dexec.mainClass=prodcons.v1.TestProdCons

│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v2/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v3/
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v4/        
│  ├─ IProdConsBuffer.java
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
├─ v5/
│  ├─ IProdConsBuffer.java   # interface étendue avec get(int k)
│  ├─ ProdConsBuffer.java
│  ├─ Message.java
│  ├─ Producer.java
│  ├─ Consumer.java
│  └─ TestProdCons.java
└─ v6/
   ├─ IProdConsBuffer.java   # interface modifiée pour supporter put(Message,int)
   ├─ ProdConsBuffer.java
   ├─ Message.java
   ├─ Producer.java
   ├─ Consumer.java
   └─ TestProdCons.java
