삼각형 $ABC$의 각 꼭짓점 좌표를 $A(x_A, y_A)$, $B(x_B, y_B)$, $C(x_C, y_C)$라고 할 때, 주어진 조건은 다음과 같습니다.

1.  $A(3, -1)$
2.  변 $AB$의 중점 $M(x_1, y_1)$
3.  변 $AC$의 중점 $N(x_2, y_2)$
4.  $x_1 + x_2 = 6$
5.  $y_1 + y_2 = -1$

**1. 중점의 정의를 이용해 $B$와 $C$의 좌표 표현하기**

중점 $M$은 $A$와 $B$의 중점이므로,
$$ M\left(x_1, y_1\right) = \left(\frac{x_A + x_B}{2}, \frac{y_A + y_B}{2}\right) $$
$$ \left(x_1, y_1\right) = \left(\frac{3 + x_B}{2}, \frac{-1 + y_B}{2}\right) $$
이를 통해 $x_B, y_B$를 $x_1, y_1$으로 표현하면:
$$ 2x_1 = 3 + x_B \implies x_B = 2x_1 - 3 $$
$$ 2y_1 = -1 + y_B \implies y_B = 2y_1 + 1 $$

마찬가지로 중점 $N$은 $A$와 $C$의 중점이므로,
$$ N\left(x_2, y_2\right) = \left(\frac{x_A + x_C}{2}, \frac{y_A + y_C}{2}\right) $$
$$ \left(x_2, y_2\right) = \left(\frac{3 + x_C}{2}, \frac{-1 + y_C}{2}\right) $$
이를 통해 $x_C, y_C$를 $x_2, y_2$로 표현하면:
$$ 2x_2 = 3 + x_C \implies x_C = 2x_2 - 3 $$
$$ 2y_2 = -1 + y_C \implies y_C = 2y_2 + 1 $$

**2. 삼각형 $ABC$의 무게중심 $(a, b)$ 구하기**

무게중심의 좌표는 세 꼭짓점의 x좌표와 y좌표의 평균으로 구할 수 있습니다.
$$ (a, b) = \left(\frac{x_A + x_B + x_C}{3}, \frac{y_A + y_B + y_C}{3}\right) $$
위에서 구한 $x_B, y_B, x_C, y_C$ 값을 대입하면:

$$ a = \frac{3 + (2x_1 - 3) + (2x_2 - 3)}{3} $$
$$ a = \frac{3 + 2x_1 - 3 + 2x_2 - 3}{3} $$
$$ a = \frac{2(x_1 + x_2) - 3}{3} $$
주어진 조건 $x_1 + x_2 = 6$을 대입하면:
$$ a = \frac{2(6) - 3}{3} = \frac{12 - 3}{3} = \frac{9}{3} = 3 $$

$$ b = \frac{-1 + (2y_1 + 1) + (2y_2 + 1)}{3} $$
$$ b = \frac{-1 + 2y_1 + 1 + 2y_2 + 1}{3} $$
$$ b = \frac{2(y_1 + y_2) + 1}{3} $$
주어진 조건 $y_1 + y_2 = -1$을 대입하면:
$$ b = \frac{2(-1) + 1}{3} = \frac{-2 + 1}{3} = \frac{-1}{3} $$

**3. $a + b$의 값 계산하기**

$$ a + b = 3 + \left(-\frac{1}{3}\right) $$
$$ a + b = 3 - \frac{1}{3} = \frac{9}{3} - \frac{1}{3} = \frac{8}{3} $$

**정답: $\frac{8}{3}$**