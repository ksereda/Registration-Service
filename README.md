# "Registration-Service" application

This is registration system, that will send a confirmation email after new user is registered.


### Stack
Netflix OSS (Eureka, Config Server), Kafka, SpringBoot, Security (JWT), H2 DB.

___

- `Eureka Server (Service registry)`

Where all services will register themselves

- `Spring Cloud Config (Config server)`

Where all services will take their configurations from.

Config server will keep configuration files in git repository.

- `Zuul (API Gateway)`

That will redirect all the requests to the needed microservice

- `User service`

This service will create a new user (using integration with Kafka).

On every new registration the User service will send a message “USER_REGISTERED” to the message broker (Kafka).

- `Email service`

This service will send a confirmation to the specified email that the user has been successfully created (using integration with Kafka)

____

1 `Netflix Eureka`:

Service instances register to the `Eureka Server`. 

`Eureka` for its part pings the registered services every 30 seconds to verify they are up and running.

Marked as

    @EnableEurekaServer

Check Eureka running on 
    
    http://localhost:8761

2 `Config server (Spring Cloud Config)`

This application will run on different environments (DEV, QA. PROD) and modify property files before the deploy is not welcomed.

It works the following way: 
property files for each service and each environment are kept in git repository. On startup every service is asking the config server for the proper configurations needed.

In the current solution we are going to build the cloud config server as a discovery service client. 

So on startup each microservice will take the config server location from the discovery service.

Marked as

    @EnableConfigServer
    
and also

    @EnableEurekaClient
    
for marked service as Eureka Client

Next step:

You need install zookeeper and Kafka.
For this you need download it from official site.

Kafka:

    https://kafka.apache.org/quickstart
    
Zookeeper:

    https://zookeeper.apache.org/releases.html
    
Download it and unzip into folder 
    
    for example: /opt/kafka_2.12-2.3.0

Open new terminal and go to kafka folder: 

    for example: cd /opt/kafka_2.12-2.3.0

First run Zookeeper without sudo roots

    ./bin/zookeeper-server-start.sh config/zookeeper.properties
    
Then open new terminal in Kafka folder and run Kafka server without sudo roots

    ./bin/kafka-server-start.sh config/server.properties

3 `User service`

This service will

   1. Register itself to the Service registry (Eureka Server)
   
   2. Take its configuration from the Config server (Spring Cloud Config)
   
   3. Have controller and two endpoints
        /register – where with POST request will register the new users
        /members – where with GET request will be able to take all registered users
   
   4. On every new registration the User service will send a message “USER_REGISTERED” to the message broker (Kafka)
   
   5. Store the registered users in memory H2 database for later reference
   
There is additional settings for this service in `bootstrap.yml` file.

To be able to produce messages for the Kafka topics we need KafkaTemplate which is the class responsible for executing high-level operations. 

The KafkaTemplate needs ProducerFactory that sets the strategy to produce a Producer instance(s). 

The ProducerFactory for its part needs a Map of configuration properties. 
The most important of which are `BOOTSTRAP_SERVERS_CONFIG`, `KEY_SERIALIZER_CLASS_CONFIG` and `VALUE_SERIALIZER_CLASS_CONFIG`.

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
     
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
     
        return props;
    }
     
    @Bean
    public ProducerFactory<String, User> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }
     
    @Bean
    public KafkaTemplate<String, User> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
     
    @Bean
    public Sender sender() {
        return new Sender();
    }
    
Here we configured to send messages to Kafka server on localhost:9092 (bootstrapServers – taken from cloud config server).

The business logic to send message to Kafka when a new user is saved in the database goes in the UserServiceImpl class.


4 `Config Properties Service`

We have Config Properties Service for store all settings for different profiles: `DEV, QA. PROD`.

He is in a remote git repository. 

All other services will access the `Config server (Spring Cloud Config)` service, which shows the path to the remote git repository, where this `Config Properties Service` service is located, and take the necessary parameters depending on the profile: `DEV, QA. PROD`.


5 `Email Service`

It will be a microservice configured to listen for the USER_CREATED_TOPIC that comes from the user service. 

Here we will build an UserDto and will configure the Kafka consumer to transform the incoming payload to it. 

Similarly to the user microservice here we will have EmailService where the business logic will be executed. 

This EmailService will be using the UserDto from the payload, will transform it to Mail entity, will save it in the database and will send the mail.

To be able to read the messages from Kafka, we need to configure `ConsumerFactory` and wrap `KafkaListenerContainerFactory`. 

As well as the `ProducerFactory` it needs some configuration properties to be set. 

`@EnableKafka` is needed to enable detection of `@KafkaListener` annotations on any Spring-managed beans.

    @Configuration
    @EnableKafka
    public class ReceiverConfig {
     
        @Value("${spring.kafka.bootstrap-servers}")
        private String bootstrapServers;
     
        @Bean
        public Map<String, Object> consumerConfigs() {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "UserCreatedConsumer");
     
            return props;
        }
     
        @Bean
        public ConsumerFactory<String, UserDto> consumerFactory() {
            return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(),
                    new JsonDeserializer<>(UserDto.class));
        }
     
        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, UserDto> kafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<String, UserDto> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory());
     
            return factory;
        }
     
        @Bean
        public Receiver receiver() {
            return new Receiver();
        }
    }

Then in the method annotated with `@KafkaListener` we add the logic we want to be invoked when a message is received.

    @KafkaListener(topics = "${spring.kafka.topic.userCreated}")
    public void receive(UserDto payload) {
       emailService.sendSimpleMessage(payload);
       latch.countDown();
    }

`EmailServiceImpl` is the place where we transform the incoming payload, send the email and save it for future reference.

Check it through `Postman`:

    POST 
    http://localhost:8081/register
    
    {
    "username": "your_email@gmail.com",
    "password": "password"
    }


6 `Zuul API Gateway`

We have a new service `Zuul`, that will be the front door for all other microservices. 

Clients will call this microservice and it will delegate the requests to the appropriate one.

It will be marked as

    @EnableEurekaClient
    @EnableZuulProxy
    
Checked

    POST 
    http://localhost:8765/api/user/register
    
    {
    "username": "your_email@gmail.com",
    "password": "password"
    }
    


