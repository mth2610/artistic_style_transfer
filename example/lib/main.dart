import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:image_picker/image_picker.dart';
import 'package:artistic_style_transfer/artistic_style_transfer.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  File _image;
  String _proceessedImage;
  bool _isSaving;
  List<int> _selectedStyles = [];
  Map<int, bool> _selectedStylesState = {
    0: false,
    1: false,
    2: false,
    3: false,
    4: false,
    5: false,
    6: false,
    7: false,
    8: false,
    9: false,
    10: false,
    11: false,
    12: false,
    13: false,
    14: false,
    15: false,
    16: false,
    17: false,
    18: false,
    19: false,
    20: false,
    21: false,
    22: false,
    23: false,
    24: false,
    25: false,
    26: false,
  };

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: _isSaving == true
        ? Center(child: CircularProgressIndicator())
        : ListView(
          children: <Widget>[
            _buildPickedImage(),
            _buildImagePickerButton(),
            _buildProcessButton(),
            Wrap(
              children: _buildFilterButtons(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPickedImage(){
    return _proceessedImage != null
    ? Container(
      margin: EdgeInsets.all(16),
      height: 300.0,
      decoration: BoxDecoration(
          border: Border.all(width: 1),
          image: DecorationImage(
            image: FileImage(File(_proceessedImage)),
          )
        )
    )
    : Container(
      margin: EdgeInsets.all(16),
      height: 300.0,
      child: _image!=null
        ? null
        : Center(
          child: Container(
            child: Text("No selected image"),
          ),
        ),
      decoration: _image!=null
        ? BoxDecoration(
          border: Border.all(width: 1),
          image: DecorationImage(
            image: FileImage(_image),
          )
        )
        : BoxDecoration(
          border: Border.all(width: 1)
        ),
    );
  }

  Widget _buildImagePickerButton(){
    return Container(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          IconButton(
            icon: Icon(Icons.camera_alt),
            onPressed: ()async{
              var image = await ImagePicker.pickImage(source: ImageSource.camera);
              setState(() {
                _image = image;
                _proceessedImage = null;
              });
            },
          ),
          IconButton(
            icon: Icon(Icons.folder),
            onPressed: ()async{
              var image = await ImagePicker.pickImage(source: ImageSource.gallery);
              setState(() {
                _image = image;
                _proceessedImage = null;
              });
            },
          )
        ],
      ),
    );
  }

  Widget _buildProcessButton(){
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16.0),
      child: RaisedButton(
        onPressed: ()async{
              setState(() {
                _isSaving = true;
              });
              if(_image!=null){
                Directory tempDir = await getTemporaryDirectory();
                String tempPath = tempDir.path;
                _proceessedImage = await ArtisticStyleTransfer.styleTransfer(styles: _selectedStyles, inputFilePath: _image.path, outputFilePath: tempPath);
                setState(() {
                  _isSaving = false;
                });
              } else {
                setState(() {
                  _isSaving = false;
                });
              }
        },
      )
    );
  }

  List<Widget> _buildFilterButtons(){
    List<Widget> buttons = [];
    for(int i=0; i< 26; i++){
      buttons.add(
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16.0),
          child: RaisedButton(
            color: _selectedStylesState[i]==true
              ? Colors.blue
              : Colors.grey,
            textColor: Colors.white,
            child: Text("$i"),
            onPressed: ()async{
              _selectedStylesState[i]= !_selectedStylesState[i];
              if(_selectedStyles.contains(i)){
                _selectedStyles.remove(i);
              } else {
                _selectedStyles.add(i);
              }
              setState(() {
              });
            },
          ),
        )
      );
    }
    return buttons;
  }
}