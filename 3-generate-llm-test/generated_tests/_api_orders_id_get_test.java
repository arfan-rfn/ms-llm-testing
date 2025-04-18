```java
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    public void getOrderById_shouldReturnOrder_whenOrderExists() throws Exception {
        Long orderId = 1L;
        Order mockOrder = new Order(orderId, "12345", "PENDING");
        ResponseEntity<Order> response = ResponseEntity.ok(mockOrder);

        given(orderService.getOrderById(orderId)).willReturn(response);

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.orderNumber", is("12345")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    public void getOrderById_shouldReturnNotFound_whenOrderDoesNotExist() throws Exception {
        Long orderId = 99L;

        given(orderService.getOrderById(orderId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getOrderById_shouldReturnBadRequest_whenInvalidId() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", "invalid"))
                .andExpect(status().isBadRequest());
    }
}
```