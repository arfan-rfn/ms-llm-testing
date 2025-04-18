```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    public void getRevenue_shouldReturnRevenue() throws Exception {
        double expectedRevenue = 12500.75;
        when(orderService.calculateTotalRevenue()).thenReturn(expectedRevenue);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/revenue"))
               .andExpect(status().isOk())
               .andExpect(content().string(String.valueOf(expectedRevenue)));
    }

    @Test
    public void getRevenue_shouldReturnInternalServerErrorWhenExceptionOccurs() throws Exception {
        when(orderService.calculateTotalRevenue()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/revenue"))
               .andExpect(status().isInternalServerError());
    }

    @Test
    public void getRevenue_shouldReturnZeroWhenNoOrdersExist() throws Exception {
        when(orderService.calculateTotalRevenue()).thenReturn(0.0);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/revenue"))
               .andExpect(status().isOk())
               .andExpect(content().string("0.0"));
    }
}
```