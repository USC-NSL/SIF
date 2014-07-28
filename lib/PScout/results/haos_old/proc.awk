BEGIN {
  FS = "<|>|:| "
}
{
  print $2"."$5
}
