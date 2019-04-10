# DiscreteSlider

DiscreteSlider tries to offer you the slider with value label as shown in the [Material Design spec](https://material.io/design/components/sliders.html), with an API. The library also offer you range slider mode.

## Screenshot
<img src="https://raw.githubusercontent.com/hearsilent/DiscreteSlider/master/screenshots/screenrecord.gif" height="500">

### Normal Mode
<img src="https://raw.githubusercontent.com/hearsilent/DiscreteSlider/master/screenshots/device-2019-04-10-215937.png" height="500">

### Range Mode
<img src="https://raw.githubusercontent.com/hearsilent/DiscreteSlider/master/screenshots/device-2019-04-10-215451.png" height="500">

## Usage
   
Setup *Track* in Java
```java
mSlider.setTrackWidth(Utils.convertDpToPixel(4, this));
mSlider.setTrackColor(0xFFD81B60);
mSlider.setInactiveTrackColor(0x3DD81B60);
```
   
Setup *Track* in Xml
```xml
app:trackWidth="4dp"
app:trackColor="#D81B60"
app:inactiveTrackColor="#3DD81B60"
```
   
Setup *Thumb* in Java
```java
mSlider.setThumbRadius(Utils.convertDpToPixel(6, this));
mSlider.setThumbColor(0xFFD81B60);
mSlider.setThumbPressedColor(0x1FD81B60);
```
   
Setup *Thumb* in Xml
```xml
app:thumbRadius="6dp"
app:thumbColor="#D81B60"
app:thumbPressedColor="#1FD81B60"
```
   
Setup *TickMark* in Java
```java
mSlider.setTickMarkColor(0x3DFFFFFF);
mSlider.setTickMarkInactiveColor(0x1FD81B60);
mSlider.setTickMarkPatterns(Arrays.asList(new Dot(), new Dash(Utils.convertDpToPixel(1, this))));
```
   
Setup *TickMark* in Xml
```xml
app:tickMarkColor="#3DFFFFFF"
app:tickMarkInactiveColor="#1FD81B60"
app:tickMarkPatterns="dot,dash" <!-- Separated by commas, only accept `dot` or `dash`. You can also fill in "dot,dash,dash"-->
app:tickMarkDashLength="1dp"
```
   
Setup *ValueLabel* in Java
```java
mSlider.setValueLabelTextColor(Color.WHITE);
mSlider.setValueLabelTextSize(Utils.convertSpToPixel(16, this));
mSlider.setValueLabelFormatter(new DiscreteSlider.ValueLabelFormatter() {

    @Override
    public String getLabel(int input) {
        return Integer.toString(input);
    }
});
```

Setup *ValueLabel* in Xml
```xml
app:valueLabelTextColor="#FFF"
app:valueLabelTextSize="16sp
```
   
Setup *Progress* in Java
```java
mSlider.setCount(21);
mSlider.setLeftProgress(5);
mSlider.setRightProgress(10); // Only can call in range mode.
```
   
Setup *Progress* in Xml
```xml
app:count="21"
app:leftProgress="5"
app:rightProgress="5"
```
   
Setup *Mode* in Java
```java
mSlider.setMode(DiscreteSlider.MODE_RANGE);
```
   
Setup *Mode* in Xml
```xml
app:mode="range"
```
   
## Compatibility

Android LOLLIPOP 5.0+

## Credits

This project was inspired by the [Material Design spec](https://material.io/design/components/sliders.html) by Google.

## License

    MIT License

    Copyright (c) 2019 HearSilent

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
