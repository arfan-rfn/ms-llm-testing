```java
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void getOrderById_shouldReturnOrder_whenOrderExists() throws Exception {
        // Given
        Long orderId = 1L;
        Order mockOrder = new Order(orderId, "Test Order", 100.0);
        ResponseEntity<Order> response = ResponseEntity.ok(mockOrder);

        given(orderService.getOrderById(orderId)).willReturn(response);

        // When
        ResultActions result = mockMvc.perform(get("/api/orders/{id}", orderId));

        // Then
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.id", is(orderId.intValue())))
              .andExpect(jsonPath("$.name", is("Test Order")))
              .andExpect(jsonPath("$.amount", is(100.0)));
    }

    @Test
    public void getOrderById_shouldReturnNotFound_whenOrderDoesNotExist() throws Exception {
        // Given
        Long orderId = 999L;
        ResponseEntity<Order> response = ResponseEntity.notFound().build();

        given(orderService.getOrderById(orderId)).willReturn(response);

        // When
        ResultActions result = mockMvc.perform(get("/api/orders/{id}", orderId));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    public void getOrderById_shouldReturnBadRequest_whenInvalidIdFormat() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get("/api/orders/{id}", "invalid"));

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void getOrderById_shouldReturnInternalServerError_whenServiceThrowsException() throws Exception {
        // Given
        Long orderId = 1L;
        given(orderService.getOrderById(orderId)).willThrow(new RuntimeException("Database error"));

        // When
        ResultActions result = mockMvc.perform(get("/api/orders/{id}", orderId));

        // Then
        result.andExpect(status().isInternalServerError());
    }
}
```