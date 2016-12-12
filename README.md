# ConvinentSnackbar
make snackbar can be showed int the top of the view
the method is :

 fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConvinentSnackbar.make(view, "This is top Convinent Snackbar", ConvinentSnackbar.LENGTH_SHORT)
                       .setActionTextColor(getResources().getColor(R.color.colorPrimaryDark)).setConvinentbarGravity(Gravity.TOP).show();
            }
        });
        
        
add setConvinentbarGravity(int gravity) to set the snackbar is shown in top or in the buttom.

now it only support Gravity.TOP or Gravity.BOTTOM
