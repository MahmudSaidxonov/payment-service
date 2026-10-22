package uz.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uz.example.payment.bootstrap.DatabaseInitializer;

@SpringBootApplication
public class PaymentApplication {

	public static void main(String[] args) {
		DatabaseInitializer.initialize();
		SpringApplication.run(PaymentApplication.class, args);
	}

}
