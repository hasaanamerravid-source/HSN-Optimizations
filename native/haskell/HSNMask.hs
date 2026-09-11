{-# LANGUAGE ForeignFunctionInterface #-}
-- Optional Haskell sidecar kernels. Built with GHC -shared when present.
-- Never required on the Minecraft frame path (see ARCHITECTURE.md).

module HSNMask where

import Data.Bits ((.&.), (.|.), shiftL)
import Foreign.C.Types
import Foreign.Ptr
import Foreign.Storable

dropF64 :: CDouble -> CDouble -> CUChar
dropF64 value limitSq = if value > limitSq then 1 else 0

fillMask :: Ptr CDouble -> CDouble -> Ptr CUChar -> CSize -> IO ()
fillMask input limitSq out n = go 0
  where
    go i
      | i >= n = return ()
      | otherwise = do
          v <- peekElemOff input (fromIntegral i)
          pokeElemOff out (fromIntegral i) (dropF64 v limitSq)
          go (i + 1)

distSq :: Ptr CDouble -> Ptr CDouble -> Ptr CDouble
       -> CDouble -> CDouble -> CDouble
       -> Ptr CDouble -> CSize -> IO ()
distSq xs ys zs ox oy oz out n = go 0
  where
    go i
      | i >= n = return ()
      | otherwise = do
          x <- peekElemOff xs (fromIntegral i)
          y <- peekElemOff ys (fromIntegral i)
          z <- peekElemOff zs (fromIntegral i)
          let dx = x - ox
              dy = y - oy
              dz = z - oz
          pokeElemOff out (fromIntegral i) (dx * dx + dy * dy + dz * dz)
          go (i + 1)

lodBand :: Ptr CDouble -> CDouble -> CDouble -> Ptr CUChar -> CSize -> IO ()
lodBand dist nearSq midSq out n = go 0
  where
    go i
      | i >= n = return ()
      | otherwise = do
          d <- peekElemOff dist (fromIntegral i)
          let band | d < nearSq = 0
                   | d < midSq  = 1
                   | otherwise  = 2
          pokeElemOff out (fromIntegral i) band
          go (i + 1)

rsqrtF64 :: Ptr CDouble -> Ptr CDouble -> CSize -> IO ()
rsqrtF64 input out n = go 0
  where
    go i
      | i >= n = return ()
      | otherwise = do
          v <- peekElemOff input (fromIntegral i)
          let y = if v > 0 then 1.0 / sqrt (realToFrac v :: Double) else 0
          pokeElemOff out (fromIntegral i) (realToFrac y)
          go (i + 1)

frustumAabb :: Ptr CFloat -> Ptr CFloat -> Ptr CUChar -> CSize -> IO ()
frustumAabb planes aabb out n
  | n == 0 = return ()
  | otherwise = go 0
  where
    outside a b c d minx miny minz maxx maxy maxz =
      let px = if a >= 0 then maxx else minx
          py = if b >= 0 then maxy else miny
          pz = if c >= 0 then maxz else minz
      in a * px + b * py + c * pz + d < 0
    go i
      | i >= n = return ()
      | otherwise = do
          let base = fromIntegral i * 6
          minx <- peekElemOff aabb base
          miny <- peekElemOff aabb (base + 1)
          minz <- peekElemOff aabb (base + 2)
          maxx <- peekElemOff aabb (base + 3)
          maxy <- peekElemOff aabb (base + 4)
          maxz <- peekElemOff aabb (base + 5)
          dropBox <- checkPlane 0 False minx miny minz maxx maxy maxz
          pokeElemOff out (fromIntegral i) (if dropBox then 1 else 0)
          go (i + 1)
    checkPlane p acc minx miny minz maxx maxy maxz
      | acc = return True
      | p >= 6 = return False
      | otherwise = do
          let o = p * 4
          a <- peekElemOff planes o
          b <- peekElemOff planes (o + 1)
          c <- peekElemOff planes (o + 2)
          d <- peekElemOff planes (o + 3)
          checkPlane (p + 1) (outside a b c d minx miny minz maxx maxy maxz)
                     minx miny minz maxx maxy maxz

part1by2 :: CUInt -> CUInt
part1by2 n0 =
  let n1 = n0 .&. 0x3ff
      n2 = (n1 .|. shiftL n1 16) .&. 0x030000FF
      n3 = (n2 .|. shiftL n2 8)  .&. 0x0300F00F
      n4 = (n3 .|. shiftL n3 4)  .&. 0x030C30C3
      n5 = (n4 .|. shiftL n4 2)  .&. 0x09249249
  in n5

morton3 :: Ptr CInt -> Ptr CInt -> Ptr CInt -> Ptr CUInt -> CSize -> IO ()
morton3 xs ys zs out n = go 0
  where
    go i
      | i >= n = return ()
      | otherwise = do
          x <- peekElemOff xs (fromIntegral i)
          y <- peekElemOff ys (fromIntegral i)
          z <- peekElemOff zs (fromIntegral i)
          let xi = part1by2 (fromIntegral x)
              yi = part1by2 (fromIntegral y)
              zi = part1by2 (fromIntegral z)
          pokeElemOff out (fromIntegral i) (xi .|. shiftL yi 1 .|. shiftL zi 2)
          go (i + 1)

keepU8 :: Ptr CUChar -> CUChar -> Ptr CUChar -> CSize -> IO ()
keepU8 rnd thresh out n = go 0
  where
    go i
      | i >= n = return ()
      | otherwise = do
          v <- peekElemOff rnd (fromIntegral i)
          pokeElemOff out (fromIntegral i) (if v < thresh then 1 else 0)
          go (i + 1)

hsn_hs_cull_f64 :: Ptr CDouble -> CDouble -> Ptr CUChar -> CSize -> IO ()
hsn_hs_cull_f64 input limitSq out n
  | n == 0 = return ()
  | otherwise = fillMask input limitSq out n

hsn_hs_dist_sq :: Ptr CDouble -> Ptr CDouble -> Ptr CDouble
               -> CDouble -> CDouble -> CDouble
               -> Ptr CDouble -> CSize -> IO ()
hsn_hs_dist_sq xs ys zs ox oy oz out n
  | n == 0 = return ()
  | otherwise = distSq xs ys zs ox oy oz out n

hsn_hs_lod_band :: Ptr CDouble -> CDouble -> CDouble -> Ptr CUChar -> CSize -> IO ()
hsn_hs_lod_band = lodBand

hsn_hs_rsqrt_f64 :: Ptr CDouble -> Ptr CDouble -> CSize -> IO ()
hsn_hs_rsqrt_f64 = rsqrtF64

hsn_hs_frustum_aabb :: Ptr CFloat -> Ptr CFloat -> Ptr CUChar -> CSize -> IO ()
hsn_hs_frustum_aabb = frustumAabb

hsn_hs_morton3 :: Ptr CInt -> Ptr CInt -> Ptr CInt -> Ptr CUInt -> CSize -> IO ()
hsn_hs_morton3 = morton3

hsn_hs_keep_u8 :: Ptr CUChar -> CUChar -> Ptr CUChar -> CSize -> IO ()
hsn_hs_keep_u8 = keepU8

hsn_hs_abi :: IO CInt
hsn_hs_abi = return 0x48534B4C -- "HSKL"

foreign export ccall hsn_hs_cull_f64 :: Ptr CDouble -> CDouble -> Ptr CUChar -> CSize -> IO ()
foreign export ccall hsn_hs_dist_sq :: Ptr CDouble -> Ptr CDouble -> Ptr CDouble -> CDouble -> CDouble -> CDouble -> Ptr CDouble -> CSize -> IO ()
foreign export ccall hsn_hs_lod_band :: Ptr CDouble -> CDouble -> CDouble -> Ptr CUChar -> CSize -> IO ()
foreign export ccall hsn_hs_rsqrt_f64 :: Ptr CDouble -> Ptr CDouble -> CSize -> IO ()
foreign export ccall hsn_hs_frustum_aabb :: Ptr CFloat -> Ptr CFloat -> Ptr CUChar -> CSize -> IO ()
foreign export ccall hsn_hs_morton3 :: Ptr CInt -> Ptr CInt -> Ptr CInt -> Ptr CUInt -> CSize -> IO ()
foreign export ccall hsn_hs_keep_u8 :: Ptr CUChar -> CUChar -> Ptr CUChar -> CSize -> IO ()
foreign export ccall hsn_hs_abi :: IO CInt
